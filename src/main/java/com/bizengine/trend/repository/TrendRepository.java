package com.bizengine.trend.repository;

import com.bizengine.trend.domain.TrendDirection;
import com.bizengine.trend.domain.TrendSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class TrendRepository {
    private final JdbcTemplate jdbc;

    public TrendRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Rebuilds a trend snapshot from authoritative signals; the snapshot is a disposable read model. */
    public int aggregate(Instant snapshotAt, int windowHours) {
        return jdbc.update("""
                WITH bounds AS (
                    SELECT ?::timestamptz AS snapshot_at, (? * interval '1 hour') AS win
                ), eligible AS (
                    SELECT s.*, b.snapshot_at, b.win
                    FROM biz_signal s CROSS JOIN bounds b
                    WHERE s.observed_at >= b.snapshot_at - (2 * b.win)
                      AND s.observed_at < b.snapshot_at
                ), grouped AS (
                    SELECT scope_type, scope_id, partner_id, signal_code,
                           count(*) FILTER (WHERE observed_at >= snapshot_at - win) AS current_count,
                           count(*) FILTER (WHERE observed_at < snapshot_at - win) AS previous_count,
                           count(DISTINCT entity_id) FILTER (WHERE observed_at >= snapshot_at - win) AS distinct_entities,
                           COALESCE(sum(base_strength * power(0.5, extract(epoch FROM (snapshot_at - observed_at)) / (half_life_hours * 3600.0)))
                               FILTER (WHERE observed_at >= snapshot_at - win), 0) AS current_strength,
                           COALESCE(sum(base_strength * power(0.5, extract(epoch FROM (snapshot_at - observed_at)) / (half_life_hours * 3600.0)))
                               FILTER (WHERE observed_at < snapshot_at - win), 0) AS previous_strength
                    FROM eligible
                    GROUP BY scope_type, scope_id, partner_id, signal_code
                )
                INSERT INTO biz_trend_snapshot (
                    id, scope_type, scope_id, partner_id, signal_code, window_hours, snapshot_at,
                    current_window_start, previous_window_start, current_signal_count, previous_signal_count,
                    current_distinct_entities, current_weighted_strength, previous_weighted_strength,
                    change_ratio, direction, algorithm_version, created_at
                )
                SELECT gen_random_uuid(), g.scope_type, g.scope_id, g.partner_id, g.signal_code, ?,
                       b.snapshot_at, b.snapshot_at - b.win, b.snapshot_at - (2 * b.win),
                       g.current_count, g.previous_count, g.distinct_entities,
                       round(g.current_strength::numeric, 6), round(g.previous_strength::numeric, 6),
                       CASE WHEN g.previous_count = 0 THEN NULL
                            ELSE round(((g.current_count - g.previous_count)::numeric / g.previous_count), 6) END,
                       CASE WHEN g.current_count > g.previous_count THEN 'UP'
                            WHEN g.current_count < g.previous_count THEN 'DOWN' ELSE 'STABLE' END,
                       1, now()
                FROM grouped g CROSS JOIN bounds b
                ON CONFLICT (scope_type, scope_id, signal_code, window_hours, snapshot_at)
                DO UPDATE SET current_window_start = EXCLUDED.current_window_start,
                    previous_window_start = EXCLUDED.previous_window_start,
                    current_signal_count = EXCLUDED.current_signal_count,
                    previous_signal_count = EXCLUDED.previous_signal_count,
                    current_distinct_entities = EXCLUDED.current_distinct_entities,
                    current_weighted_strength = EXCLUDED.current_weighted_strength,
                    previous_weighted_strength = EXCLUDED.previous_weighted_strength,
                    change_ratio = EXCLUDED.change_ratio, direction = EXCLUDED.direction,
                    algorithm_version = EXCLUDED.algorithm_version, created_at = now()
                """, java.sql.Timestamp.from(snapshotAt), windowHours, windowHours);
    }

    public List<TrendSnapshot> findLatestForPartner(UUID partnerId, int windowHours, int limit) {
        return jdbc.query("""
                SELECT DISTINCT ON (scope_type, scope_id, signal_code)
                    id, scope_type, scope_id, signal_code, window_hours, snapshot_at,
                    current_signal_count, previous_signal_count, current_distinct_entities,
                    current_weighted_strength, previous_weighted_strength, change_ratio,
                    direction, algorithm_version
                FROM biz_trend_snapshot
                WHERE partner_id = ? AND scope_type = 'PARTNER' AND scope_id = ? AND window_hours = ?
                ORDER BY scope_type, scope_id, signal_code, snapshot_at DESC
                LIMIT ?
                """, this::map, partnerId, partnerId, windowHours, limit);
    }

    private TrendSnapshot map(ResultSet rs, int rowNum) throws SQLException {
        return new TrendSnapshot(rs.getObject("id", UUID.class), rs.getString("scope_type"),
                rs.getObject("scope_id", UUID.class), rs.getString("signal_code"), rs.getInt("window_hours"),
                rs.getTimestamp("snapshot_at").toInstant(), rs.getLong("current_signal_count"),
                rs.getLong("previous_signal_count"), rs.getLong("current_distinct_entities"),
                rs.getBigDecimal("current_weighted_strength"), rs.getBigDecimal("previous_weighted_strength"),
                rs.getBigDecimal("change_ratio"), TrendDirection.valueOf(rs.getString("direction")),
                rs.getInt("algorithm_version"));
    }
}
