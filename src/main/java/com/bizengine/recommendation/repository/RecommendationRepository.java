package com.bizengine.recommendation.repository;

import com.bizengine.recommendation.domain.RecommendationPriority;
import com.bizengine.recommendation.domain.RecommendationRecord;
import com.bizengine.recommendation.domain.RecommendationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class RecommendationRepository {
    private final JdbcTemplate jdbc;

    public RecommendationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record InsightCandidate(UUID insightId, UUID partnerId, String signalCode, String direction,
                                   String title, String summary, java.math.BigDecimal confidence,
                                   String evidenceQuality, Instant expiresAt) { }

    public List<InsightCandidate> findEligibleInsights(Instant now, int limit) {
        return jdbc.query("""
                SELECT i.id, i.partner_id, i.signal_code, i.direction, i.title, i.summary,
                       i.confidence, i.evidence_quality, i.expires_at
                FROM biz_insight i
                WHERE i.status = 'ACTIVE' AND i.expires_at > ?
                  AND i.evidence_quality IN ('MEDIUM', 'HIGH')
                  AND i.confidence >= 0.55
                ORDER BY i.updated_at DESC
                LIMIT ?
                """, (rs, row) -> new InsightCandidate(rs.getObject("id", UUID.class),
                        rs.getObject("partner_id", UUID.class), rs.getString("signal_code"), rs.getString("direction"),
                        rs.getString("title"), rs.getString("summary"), rs.getBigDecimal("confidence"),
                        rs.getString("evidence_quality"), rs.getTimestamp("expires_at").toInstant()),
                Timestamp.from(now), limit);
    }

    @Transactional
    public void upsert(InsightCandidate c, String code, String objective, String title, String rationale,
                       String action, RecommendationPriority priority, Instant validUntil) {
        jdbc.update("""
                INSERT INTO biz_recommendation
                    (id, partner_id, source_insight_id, recommendation_code, objective, title, rationale,
                     suggested_action, priority, confidence, evidence_quality, status, valid_until, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PROPOSED', ?, now(), now())
                ON CONFLICT (partner_id, source_insight_id, recommendation_code)
                DO UPDATE SET objective = EXCLUDED.objective, title = EXCLUDED.title,
                    rationale = EXCLUDED.rationale, suggested_action = EXCLUDED.suggested_action,
                    priority = EXCLUDED.priority, confidence = EXCLUDED.confidence,
                    evidence_quality = EXCLUDED.evidence_quality,
                    status = CASE WHEN biz_recommendation.status IN ('ACCEPTED', 'DISMISSED')
                                  THEN biz_recommendation.status ELSE 'PROPOSED' END,
                    valid_until = EXCLUDED.valid_until, updated_at = now()
                """, UUID.randomUUID(), c.partnerId(), c.insightId(), code, objective, title, rationale,
                action, priority.name(), c.confidence(), c.evidenceQuality(), Timestamp.from(validUntil));
    }

    public int expire(Instant now) {
        return jdbc.update("UPDATE biz_recommendation SET status = 'EXPIRED', updated_at = now() " +
                "WHERE status = 'PROPOSED' AND valid_until <= ?", Timestamp.from(now));
    }

    public List<RecommendationRecord> findForPartner(UUID partnerId, RecommendationStatus status, int limit) {
        return jdbc.query("""
                SELECT id, partner_id, source_insight_id, recommendation_code, objective, title, rationale,
                       suggested_action, priority, confidence, evidence_quality, status, valid_until, created_at, updated_at
                FROM biz_recommendation
                WHERE partner_id = ? AND (CAST(? AS VARCHAR) IS NULL OR status = CAST(? AS VARCHAR))
                ORDER BY CASE priority WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC, updated_at DESC
                LIMIT ?
                """, (rs, row) -> map(rs), partnerId, status == null ? null : status.name(),
                status == null ? null : status.name(), limit);
    }

    private RecommendationRecord map(ResultSet rs) throws SQLException {
        return new RecommendationRecord(rs.getObject("id", UUID.class), rs.getObject("partner_id", UUID.class),
                rs.getObject("source_insight_id", UUID.class), rs.getString("recommendation_code"),
                rs.getString("objective"), rs.getString("title"), rs.getString("rationale"),
                rs.getString("suggested_action"), RecommendationPriority.valueOf(rs.getString("priority")),
                rs.getBigDecimal("confidence"), rs.getString("evidence_quality"),
                RecommendationStatus.valueOf(rs.getString("status")), rs.getTimestamp("valid_until").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
