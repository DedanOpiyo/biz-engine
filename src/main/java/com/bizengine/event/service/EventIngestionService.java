package com.bizengine.event.service;

import com.bizengine.authorization.domain.AuthorizationPermission;
import com.bizengine.authorization.domain.AuthorizationScopeType;
import com.bizengine.authorization.service.PartnerAuthorizationService;
import com.bizengine.event.api.IngestEventRequest;
import com.bizengine.event.domain.OperationalEvent;
import com.bizengine.event.repository.OperationalEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;

@Service
public class EventIngestionService {
    private final PartnerAuthorizationService authorizationService;
    private final OperationalEventRepository eventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public EventIngestionService(PartnerAuthorizationService authorizationService,
                                 OperationalEventRepository eventRepository,
                                 JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.eventRepository = eventRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestResult ingest(UUID authenticatedPartnerId, String idempotencyKey, IngestEventRequest request) {
        if (authenticatedPartnerId == null) {
            throw new com.bizengine.authorization.service.AuthorizationDeniedException(
                    "AUTHENTICATION_REQUIRED", "An authenticated partner is required.");
        }
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{16,128}")) {
            throw new com.bizengine.shared.error.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must contain 16–128 safe characters.");
        }
        if (request == null || request.applicationId() == null || request.eventType() == null || request.eventType().isBlank()
                || request.schemaVersion() == null || request.schemaVersion() < 1 || request.occurredAt() == null
                || request.payload() == null || !request.payload().isObject()
                || (request.context() != null && !request.context().isObject())) {
            throw new com.bizengine.shared.error.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_EVENT", "Event fields are invalid; payload and context must be JSON objects.");
        }
        authorizationService.requireExactScopePermission(authenticatedPartnerId, authenticatedPartnerId,
                AuthorizationPermission.EVENT_INGEST, AuthorizationScopeType.APPLICATION,
                request.applicationId().toString());

        String requestHash = sha256(canonicalRequest(request));
        Instant receivedAt = Instant.now();
        UUID eventId = UUID.randomUUID();
        String contextJson = jsonOrNull(request.context());
        String payloadJson = jsonOrNull(request.payload());

        // PostgreSQL ON CONFLICT makes concurrent retries safe: only one insert wins.
        List<UUID> insertedIds = jdbcTemplate.query(
                """
                INSERT INTO biz_event (id, partner_id, application_id, idempotency_key, request_hash,
                    event_type, schema_version, occurred_at, subject_type, subject_reference,
                    context_data, payload, received_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?)
                ON CONFLICT (partner_id, idempotency_key) DO NOTHING
                RETURNING id
                """,
                (rs, rowNum) -> rs.getObject(1, UUID.class),
                eventId, authenticatedPartnerId, request.applicationId(), idempotencyKey, requestHash,
                request.eventType(), request.schemaVersion(), request.occurredAt(), request.subjectType(),
                request.subjectReference(), contextJson, payloadJson, receivedAt);

        if (!insertedIds.isEmpty()) {
            return new IngestResult(eventId, request.applicationId(), request.eventType(),
                    request.occurredAt(), receivedAt, false);
        }

        OperationalEvent existing = eventRepository.findByPartnerIdAndIdempotencyKey(authenticatedPartnerId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Idempotency conflict occurred but the original event was not found."));
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new EventIdempotencyConflictException();
        }
        return new IngestResult(existing.getId(), existing.getApplicationId(), existing.getEventType(),
                existing.getOccurredAt(), existing.getReceivedAt(), true);
    }

    private String canonicalRequest(IngestEventRequest request) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("applicationId", request.applicationId().toString());
            root.put("eventType", request.eventType());
            root.put("schemaVersion", request.schemaVersion());
            root.put("occurredAt", request.occurredAt().toString());
            if (request.subjectType() == null) root.putNull("subjectType"); else root.put("subjectType", request.subjectType());
            if (request.subjectReference() == null) root.putNull("subjectReference"); else root.put("subjectReference", request.subjectReference());
            root.set("context", canonicalize(request.context()));
            root.set("payload", canonicalize(request.payload()));
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Event could not be canonicalized.", ex);
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isNull()) return com.fasterxml.jackson.databind.node.NullNode.getInstance();
        if (node.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            for (String name : names) sorted.set(name, canonicalize(node.get(name)));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            for (JsonNode child : node) array.add(canonicalize(child));
            return array;
        }
        return node.deepCopy();
    }

    private String jsonOrNull(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) return null;
        try { return objectMapper.writeValueAsString(node); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Event JSON could not be serialized.", ex); }
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 is unavailable.", ex); }
    }

    public record IngestResult(UUID eventId, UUID applicationId, String eventType,
                               Instant occurredAt, Instant receivedAt, boolean duplicate) { }
}
