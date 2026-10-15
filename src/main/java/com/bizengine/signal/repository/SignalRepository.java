package com.bizengine.signal.repository;

import com.bizengine.signal.domain.SignalRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class SignalRepository {
    private final JdbcTemplate jdbc;

    public SignalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SignalRule> findActiveRules(String eventType) {
        return jdbc.query("""
                SELECT rule_code, rule_version, event_type, signal_code, base_strength,
                       half_life_hours, ttl_hours
                FROM biz_signal_rule WHERE active = TRUE AND event_type = ?
                ORDER BY rule_code, rule_version
                """, (rs, rowNum) -> mapRule(rs), eventType);
    }

    public int insertIfAbsent(UUID observationId, UUID eventId, UUID partnerId, UUID applicationId,
                              UUID entityId, String scopeType, UUID scopeId, SignalRule rule,
                              String signalDataJson, java.time.Instant observedAt, java.time.Instant expiresAt) {
        return jdbc.update("""
                INSERT INTO biz_signal (id, observation_id, evidence_event_id, partner_id, application_id,
                    entity_id, scope_type, scope_id, signal_code, rule_code, rule_version, base_strength,
                    half_life_hours, signal_data, observed_at, generated_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, now(), ?)
                ON CONFLICT (observation_id, rule_code, rule_version, scope_type, scope_id) DO NOTHING
                """, UUID.randomUUID(), observationId, eventId, partnerId, applicationId, entityId,
                scopeType, scopeId, rule.signalCode(), rule.ruleCode(), rule.ruleVersion(), rule.baseStrength(),
                rule.halfLifeHours(), signalDataJson, observedAt, expiresAt);
    }

    private SignalRule mapRule(ResultSet rs) throws SQLException {
        return new SignalRule(rs.getString("rule_code"), rs.getInt("rule_version"),
                rs.getString("event_type"), rs.getString("signal_code"), rs.getBigDecimal("base_strength"),
                rs.getInt("half_life_hours"), rs.getInt("ttl_hours"));
    }
}
