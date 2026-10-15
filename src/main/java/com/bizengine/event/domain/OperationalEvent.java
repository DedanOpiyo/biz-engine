package com.bizengine.event.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "biz_event")
public class OperationalEvent {
    @Id
    private UUID id;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Column(name = "event_type", nullable = false, updatable = false, length = 120)
    private String eventType;

    @Column(name = "schema_version", nullable = false, updatable = false)
    private int schemaVersion;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "subject_type", updatable = false, length = 100)
    private String subjectType;

    @Column(name = "subject_reference", updatable = false, length = 200)
    private String subjectReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "jsonb", updatable = false)
    private JsonNode contextData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode payload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected OperationalEvent() { }

    public OperationalEvent(UUID id, UUID partnerId, UUID applicationId, String idempotencyKey,
                            String requestHash, String eventType, int schemaVersion, Instant occurredAt,
                            String subjectType, String subjectReference, JsonNode contextData,
                            JsonNode payload, Instant receivedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.applicationId = applicationId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.occurredAt = occurredAt;
        this.subjectType = subjectType;
        this.subjectReference = subjectReference;
        this.contextData = contextData;
        this.payload = payload;
        this.receivedAt = receivedAt;
    }

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public UUID getApplicationId() { return applicationId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getEventType() { return eventType; }
    public int getSchemaVersion() { return schemaVersion; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectReference() { return subjectReference; }
    public JsonNode getContextData() { return contextData; }
    public JsonNode getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
}
