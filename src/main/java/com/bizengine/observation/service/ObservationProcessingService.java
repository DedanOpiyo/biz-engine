package com.bizengine.observation.service;

import com.bizengine.context.domain.ContextNormalizer;
import com.bizengine.identity.service.EntityResolutionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Converts an accepted event into a context record and a single observation. */
@Service
public class ObservationProcessingService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EntityResolutionService entityResolutionService;
    private final ContextNormalizer contextNormalizer;

    public ObservationProcessingService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                                        EntityResolutionService entityResolutionService,
                                        ContextNormalizer contextNormalizer) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.entityResolutionService = entityResolutionService;
        this.contextNormalizer = contextNormalizer;
    }

    @Transactional
    public boolean process(JsonNode envelope) {
        UUID eventId = uuid(envelope, "eventId");
        UUID partnerId = uuid(envelope, "partnerId");
        UUID applicationId = uuid(envelope, "applicationId");
        String eventType = requiredText(envelope, "eventType");
        Instant occurredAt = Instant.parse(requiredText(envelope, "occurredAt"));

        List<UUID> existing = jdbcTemplate.query("SELECT id FROM biz_observation WHERE event_id = ?",
                (rs, rowNum) -> rs.getObject(1, UUID.class), eventId);
        if (!existing.isEmpty()) return false;

        String subjectType = textOrNull(envelope.get("subjectType"));
        String subjectReference = textOrNull(envelope.get("subjectReference"));
        UUID entityId = entityResolutionService.resolveOrCreate(partnerId, applicationId, subjectType, subjectReference);
        JsonNode sourceContext = envelope.get("context");
        ObjectNode normalized = contextNormalizer.normalize(sourceContext);
        Instant now = Instant.now();
        UUID contextId = UUID.randomUUID();

        List<UUID> insertedContext = jdbcTemplate.query("""
                INSERT INTO biz_context (id, event_id, partner_id, application_id, context_version,
                    normalized_context, normalization_status, created_at)
                VALUES (?, ?, ?, ?, 1, CAST(? AS jsonb), ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                RETURNING id
                """, (rs, rowNum) -> rs.getObject(1, UUID.class), contextId, eventId, partnerId,
                applicationId, json(normalized), sourceContext == null || sourceContext.isNull() ? "UNAVAILABLE" : "NORMALIZED", now);
        if (insertedContext.isEmpty()) {
            contextId = jdbcTemplate.queryForObject("SELECT id FROM biz_context WHERE event_id = ?", UUID.class, eventId);
        } else {
            contextId = insertedContext.get(0);
        }

        int inserted = jdbcTemplate.update("""
                INSERT INTO biz_observation (id, event_id, entity_id, context_id, partner_id, application_id,
                    observation_type, observed_at, created_at, processing_version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                ON CONFLICT (event_id) DO NOTHING
                """, UUID.randomUUID(), eventId, entityId, contextId, partnerId, applicationId,
                eventType, occurredAt, now);
        return inserted == 1;
    }

    private UUID uuid(JsonNode root, String field) {
        String value = requiredText(root, field);
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid UUID field: " + field, ex); }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing or invalid event field: " + field);
        }
        return value.asText();
    }

    private String textOrNull(JsonNode value) {
        return value == null || value.isNull() || !value.isTextual() || value.asText().isBlank() ? null : value.asText();
    }

    private String json(JsonNode node) {
        try { return objectMapper.writeValueAsString(node); }
        catch (Exception ex) { throw new IllegalArgumentException("Context could not be serialized.", ex); }
    }
}
