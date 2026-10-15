package com.bizengine.signal.service;

import com.bizengine.signal.domain.SignalRule;
import com.bizengine.signal.repository.SignalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Applies reviewed deterministic rules to persisted observations. Unknown event types produce no signal. */
@Service
public class SignalGenerationService {
    private final JdbcTemplate jdbc;
    private final SignalRepository signalRepository;
    private final ObjectMapper objectMapper;

    public SignalGenerationService(JdbcTemplate jdbc, SignalRepository signalRepository, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.signalRepository = signalRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int generateForObservation(UUID observationId) {
        List<ObservationFacts> rows = jdbc.query("""
                SELECT o.id AS observation_id, o.event_id, o.partner_id, o.application_id, o.entity_id,
                       o.observation_type, o.observed_at
                FROM biz_observation o WHERE o.id = ?
                """, (rs, rowNum) -> new ObservationFacts(
                rs.getObject("observation_id", UUID.class), rs.getObject("event_id", UUID.class),
                rs.getObject("partner_id", UUID.class), rs.getObject("application_id", UUID.class),
                rs.getObject("entity_id", UUID.class), rs.getString("observation_type"),
                rs.getTimestamp("observed_at").toInstant()), observationId);
        if (rows.isEmpty()) return 0;
        ObservationFacts facts = rows.get(0);
        List<SignalRule> rules = signalRepository.findActiveRules(facts.eventType());
        int inserted = 0;
        for (SignalRule rule : rules) {
            Instant expiry = facts.observedAt().plusSeconds(Math.multiplyExact((long) rule.ttlHours(), 3600L));
            String evidenceJson = evidenceJson(facts, rule);
            // Emit entity scope only when resolution found an entity; app and partner scopes are always available.
            if (facts.entityId() != null) inserted += insert(facts, rule, "ENTITY", facts.entityId(), evidenceJson, expiry);
            inserted += insert(facts, rule, "APPLICATION", facts.applicationId(), evidenceJson, expiry);
            inserted += insert(facts, rule, "PARTNER", facts.partnerId(), evidenceJson, expiry);
        }
        return inserted;
    }

    private int insert(ObservationFacts facts, SignalRule rule, String scopeType, UUID scopeId,
                       String signalDataJson, Instant expiresAt) {
        return signalRepository.insertIfAbsent(facts.observationId(), facts.eventId(), facts.partnerId(),
                facts.applicationId(), facts.entityId(), scopeType, scopeId, rule, signalDataJson,
                facts.observedAt(), expiresAt);
    }

    private String evidenceJson(ObservationFacts facts, SignalRule rule) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("evidenceEventId", facts.eventId().toString());
        data.put("observationId", facts.observationId().toString());
        data.put("eventType", facts.eventType());
        data.put("ruleCode", rule.ruleCode());
        data.put("ruleVersion", rule.ruleVersion());
        try { return objectMapper.writeValueAsString(data); }
        catch (Exception ex) { throw new IllegalStateException("Could not serialize signal evidence.", ex); }
    }

    private record ObservationFacts(UUID observationId, UUID eventId, UUID partnerId, UUID applicationId,
                                    UUID entityId, String eventType, Instant observedAt) { }
}
