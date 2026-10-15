package com.bizengine.insight.repository;

import com.bizengine.insight.domain.EvidenceQuality;
import com.bizengine.insight.domain.InsightEvidence;
import com.bizengine.insight.domain.InsightRecord;
import com.bizengine.insight.domain.InsightStatus;
import com.bizengine.insight.domain.InsightSourceEvidence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class InsightRepository {
    private final JdbcTemplate jdbc;

    public InsightRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Candidate(UUID trendId, UUID partnerId, UUID scopeId, String signalCode, int windowHours,
                            Instant snapshotAt, long currentCount, long previousCount, long distinctEntities,
                            String direction) { }

    public List<Candidate> findRecentCandidates(Instant since, int limit) {
        return jdbc.query("""
                WITH ranked AS (
                    SELECT t.id, t.partner_id, t.scope_id, t.signal_code, t.window_hours, t.snapshot_at,
                           t.current_signal_count, t.previous_signal_count, t.current_distinct_entities, t.direction,
                           row_number() OVER (PARTITION BY t.partner_id, t.scope_id, t.signal_code, t.window_hours
                                              ORDER BY t.snapshot_at DESC) AS rn
                    FROM biz_trend_snapshot t
                    JOIN biz_insight_rule r ON r.active = TRUE AND r.signal_code IN ('*', t.signal_code)
                        AND r.direction = t.direction
                        AND t.current_signal_count >= r.minimum_signal_count
                        AND t.current_distinct_entities >= r.minimum_distinct_entities
                    WHERE t.scope_type = 'PARTNER' AND t.snapshot_at >= ?
                      AND t.snapshot_at + (GREATEST(24, t.window_hours) * interval '1 hour') > now()
                      AND t.direction IN ('UP', 'DOWN')
                )
                SELECT id, partner_id, scope_id, signal_code, window_hours, snapshot_at,
                       current_signal_count, previous_signal_count, current_distinct_entities, direction
                FROM ranked WHERE rn = 1 ORDER BY snapshot_at DESC LIMIT ?
                """, (rs, row) -> new Candidate(rs.getObject("id", UUID.class),
                        rs.getObject("partner_id", UUID.class), rs.getObject("scope_id", UUID.class),
                        rs.getString("signal_code"), rs.getInt("window_hours"), rs.getTimestamp("snapshot_at").toInstant(),
                        rs.getLong("current_signal_count"), rs.getLong("previous_signal_count"),
                        rs.getLong("current_distinct_entities"), rs.getString("direction")),
                Timestamp.from(since), limit);
    }

    @Transactional
    public UUID upsert(Candidate c, String ruleCode, int ruleVersion, String title, String summary,
                       BigDecimal confidence, EvidenceQuality quality, Instant expiresAt) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO biz_insight (id, partner_id, scope_type, scope_id, signal_code, window_hours,
                    rule_code, rule_version, title, summary, direction, confidence, evidence_quality, status,
                    source_trend_id, first_observed_at, last_evaluated_at, expires_at, created_at, updated_at)
                VALUES (?, ?, 'PARTNER', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, now(), ?, now(), now())
                ON CONFLICT (partner_id, scope_type, scope_id, signal_code, window_hours, rule_code)
                DO UPDATE SET rule_version = EXCLUDED.rule_version, title = EXCLUDED.title, summary = EXCLUDED.summary,
                    direction = EXCLUDED.direction, confidence = EXCLUDED.confidence,
                    evidence_quality = EXCLUDED.evidence_quality, status = 'ACTIVE',
                    source_trend_id = EXCLUDED.source_trend_id, last_evaluated_at = now(),
                    expires_at = EXCLUDED.expires_at, updated_at = now()
                RETURNING id
                """, UUID.class, UUID.randomUUID(), c.partnerId(), c.scopeId(), c.signalCode(), c.windowHours(),
                ruleCode, ruleVersion, title, summary, c.direction(), confidence, quality.name(), c.trendId(),
                Timestamp.from(c.snapshotAt()), Timestamp.from(expiresAt));
        jdbc.update("""
                INSERT INTO biz_insight_evidence (id, insight_id, trend_snapshot_id, evidence_role)
                VALUES (?, ?, ?, 'PRIMARY_TREND') ON CONFLICT (insight_id, trend_snapshot_id, evidence_role) DO NOTHING
                """, UUID.randomUUID(), id, c.trendId());
        jdbc.update("""
                INSERT INTO biz_insight_source_evidence (id, insight_id, signal_id, observation_id, event_id, recorded_at)
                SELECT gen_random_uuid(), ?, s.id, s.observation_id, s.evidence_event_id, now()
                FROM biz_signal s
                WHERE s.partner_id = ? AND s.scope_type = 'PARTNER' AND s.scope_id = ?
                  AND s.signal_code = ? AND s.observed_at >= ? - (? * interval '1 hour')
                  AND s.observed_at < ?
                ORDER BY s.observed_at DESC LIMIT 20
                ON CONFLICT (insight_id, signal_id) DO NOTHING
                """, id, c.partnerId(), c.scopeId(), c.signalCode(), Timestamp.from(c.snapshotAt()),
                c.windowHours(), Timestamp.from(c.snapshotAt()));
        String oppositeRule = "UP".equals(c.direction()) ? "SIGNAL_ACTIVITY_DECREASE" : "SIGNAL_ACTIVITY_INCREASE";
        jdbc.update("""
                UPDATE biz_insight SET status = 'SUPERSEDED', updated_at = now()
                WHERE partner_id = ? AND scope_type = 'PARTNER' AND scope_id = ? AND signal_code = ?
                  AND window_hours = ? AND rule_code = ? AND status = 'ACTIVE' AND id <> ?
                """, c.partnerId(), c.scopeId(), c.signalCode(), c.windowHours(), oppositeRule, id);
        return id;
    }

    public int expire(Instant now) {
        return jdbc.update("UPDATE biz_insight SET status = 'EXPIRED', updated_at = now() WHERE status = 'ACTIVE' AND expires_at <= ?",
                Timestamp.from(now));
    }

    public List<InsightRecord> findForPartner(UUID partnerId, InsightStatus status, int limit) {
        List<InsightRecord> rows = jdbc.query("""
                SELECT id, partner_id, scope_type, scope_id, signal_code, window_hours, rule_code, rule_version,
                       title, summary, direction, confidence, evidence_quality, status, first_observed_at,
                       last_evaluated_at, expires_at
                FROM biz_insight WHERE partner_id = ? AND (CAST(? AS VARCHAR) IS NULL OR status = CAST(? AS VARCHAR))
                ORDER BY updated_at DESC LIMIT ?
                """, (rs, row) -> map(rs), partnerId, status == null ? null : status.name(),
                status == null ? null : status.name(), limit);
        List<InsightRecord> withEvidence = new ArrayList<>(rows.size());
        for (InsightRecord row : rows) {
            List<InsightEvidence> evidence = jdbc.query("""
                    SELECT e.trend_snapshot_id, t.signal_code, t.window_hours, t.snapshot_at,
                           t.current_signal_count, t.previous_signal_count, t.current_distinct_entities,
                           t.direction, e.evidence_role
                    FROM biz_insight_evidence e JOIN biz_trend_snapshot t ON t.id = e.trend_snapshot_id
                    JOIN biz_insight i ON i.id = e.insight_id AND i.partner_id = t.partner_id
                    WHERE e.insight_id = ? ORDER BY e.recorded_at DESC LIMIT 20
                    """, (rs, n) -> new InsightEvidence(rs.getObject("trend_snapshot_id", UUID.class),
                    rs.getString("signal_code"), rs.getInt("window_hours"), rs.getTimestamp("snapshot_at").toInstant(),
                    rs.getLong("current_signal_count"), rs.getLong("previous_signal_count"),
                    rs.getLong("current_distinct_entities"), rs.getString("direction"), rs.getString("evidence_role")), row.id());
            List<InsightSourceEvidence> sourceEvidence = jdbc.query("""
                    SELECT s.signal_id, s.observation_id, s.event_id, bs.signal_code, bs.observed_at, bs.base_strength
                    FROM biz_insight_source_evidence s JOIN biz_signal bs ON bs.id = s.signal_id
                    JOIN biz_observation o ON o.id = s.observation_id AND o.partner_id = bs.partner_id
                    JOIN biz_event e ON e.id = s.event_id AND e.partner_id = bs.partner_id
                    JOIN biz_insight i ON i.id = s.insight_id AND i.partner_id = bs.partner_id
                    WHERE s.insight_id = ? ORDER BY s.recorded_at DESC LIMIT 20
                    """, (rs, n) -> new InsightSourceEvidence(rs.getObject("signal_id", UUID.class),
                    rs.getObject("observation_id", UUID.class), rs.getObject("event_id", UUID.class),
                    rs.getString("signal_code"), rs.getTimestamp("observed_at").toInstant(), rs.getBigDecimal("base_strength")), row.id());
            withEvidence.add(new InsightRecord(row.id(), row.partnerId(), row.scopeType(), row.scopeId(), row.signalCode(),
                    row.windowHours(), row.ruleCode(), row.ruleVersion(), row.title(), row.summary(), row.direction(),
                    row.confidence(), row.evidenceQuality(), row.status(), row.firstObservedAt(), row.lastEvaluatedAt(),
                    row.expiresAt(), evidence, sourceEvidence));
        }
        return withEvidence;
    }

    private InsightRecord map(ResultSet rs) throws SQLException {
        return new InsightRecord(rs.getObject("id", UUID.class), rs.getObject("partner_id", UUID.class),
                rs.getString("scope_type"), rs.getObject("scope_id", UUID.class), rs.getString("signal_code"),
                rs.getInt("window_hours"), rs.getString("rule_code"), rs.getInt("rule_version"),
                rs.getString("title"), rs.getString("summary"), rs.getString("direction"),
                rs.getBigDecimal("confidence"), EvidenceQuality.valueOf(rs.getString("evidence_quality")),
                InsightStatus.valueOf(rs.getString("status")), rs.getTimestamp("first_observed_at").toInstant(),
                rs.getTimestamp("last_evaluated_at").toInstant(), rs.getTimestamp("expires_at").toInstant(), List.of(), List.of());
    }
}
