package com.bizengine.outbox.service;

import com.bizengine.outbox.domain.OutboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxDispatchService {
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final long sendTimeoutMs;
    private final int maxAttempts;
    private final long baseBackoffMs;
    private final long maxBackoffMs;
    private final String deadLetterTopic;

    public OutboxDispatchService(JdbcTemplate jdbcTemplate,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${biz.outbox.batch-size:50}") int batchSize,
                                 @Value("${biz.outbox.send-timeout-ms:3000}") long sendTimeoutMs,
                                 @Value("${biz.outbox.max-attempts:10}") int maxAttempts,
                                 @Value("${biz.outbox.base-backoff-ms:1000}") long baseBackoffMs,
                                 @Value("${biz.outbox.max-backoff-ms:300000}") long maxBackoffMs,
                                 @Value("${biz.outbox.dead-letter-topic:biz.events.dlq}") String deadLetterTopic) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = Math.max(1, batchSize);
        this.sendTimeoutMs = Math.max(1, sendTimeoutMs);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseBackoffMs = Math.max(1, baseBackoffMs);
        this.maxBackoffMs = Math.max(this.baseBackoffMs, maxBackoffMs);
        this.deadLetterTopic = deadLetterTopic;
    }

    /**
     * At-least-once publication: the database row is marked only after Kafka acknowledges.
     * A crash between Kafka acknowledgement and DB commit can cause a duplicate; consumers
     * must deduplicate using the stable outbox/event ID.
     */
    @Transactional
    public int publishBatch() {
        List<OutboxMessage> messages = jdbcTemplate.query("""
                SELECT id, aggregate_id, event_type, topic, message_key, payload::text, attempts
                FROM biz_outbox_event
                WHERE status = 'PENDING' AND next_attempt_at <= now()
                ORDER BY created_at
                LIMIT ? FOR UPDATE SKIP LOCKED
                """, (rs, rowNum) -> new OutboxMessage(
                rs.getObject("id", java.util.UUID.class),
                rs.getObject("aggregate_id", java.util.UUID.class),
                rs.getString("event_type"), rs.getString("topic"), rs.getString("message_key"),
                rs.getString("payload"), rs.getInt("attempts")), batchSize);

        int processed = 0;
        for (OutboxMessage message : messages) {
            try {
                if (message.attempts() >= maxAttempts) {
                    sendToDeadLetter(message, "Maximum publication attempts reached.");
                    processed++;
                    continue;
                }
                kafkaTemplate.send(message.topic(), message.messageKey(), message.payload())
                        .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                jdbcTemplate.update("""
                        UPDATE biz_outbox_event
                        SET status = 'PUBLISHED', published_at = ?, last_error = NULL
                        WHERE id = ? AND status = 'PENDING'
                        """, Instant.now(), message.id());
            } catch (Exception exception) {
                int attempts = message.attempts() + 1;
                String error = conciseError(exception);
                if (message.attempts() >= maxAttempts) {
                    // The DLQ itself is unavailable; keep the row durable and retry later.
                    scheduleRetry(message.id(), attempts, error);
                } else if (attempts >= maxAttempts) {
                    try {
                        sendToDeadLetter(message, error);
                    } catch (Exception dlqException) {
                        scheduleRetry(message.id(), attempts, conciseError(dlqException));
                    }
                } else {
                    scheduleRetry(message.id(), attempts, error);
                }
            }
            processed++;
        }
        return processed;
    }

    private void sendToDeadLetter(OutboxMessage message, String reason) throws Exception {
        ObjectNode deadLetter = objectMapper.createObjectNode();
        deadLetter.put("outboxId", message.id().toString());
        deadLetter.put("aggregateId", message.aggregateId().toString());
        deadLetter.put("eventType", message.eventType());
        deadLetter.put("originalTopic", message.topic());
        deadLetter.put("reason", reason);
        deadLetter.put("failedAt", Instant.now().toString());
        deadLetter.set("originalPayload", objectMapper.readTree(message.payload()));
        kafkaTemplate.send(deadLetterTopic, message.id().toString(), objectMapper.writeValueAsString(deadLetter))
                .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
        jdbcTemplate.update("""
                UPDATE biz_outbox_event
                SET status = 'DEAD', dead_lettered_at = ?, attempts = ?, last_error = ?
                WHERE id = ? AND status = 'PENDING'
                """, Instant.now(), Math.max(maxAttempts, message.attempts()), truncate(reason), message.id());
    }

    private void scheduleRetry(java.util.UUID id, int attempts, String error) {
        long exponent = Math.min(30, Math.max(0, attempts - 1));
        long delay;
        try {
            delay = Math.min(maxBackoffMs, Math.multiplyExact(baseBackoffMs, 1L << exponent));
        } catch (ArithmeticException overflow) {
            delay = maxBackoffMs;
        }
        jdbcTemplate.update("""
                UPDATE biz_outbox_event
                SET attempts = ?, last_error = ?, next_attempt_at = ?
                WHERE id = ? AND status = 'PENDING'
                """, attempts, truncate(error), Instant.now().plusMillis(delay), id);
    }

    private String conciseError(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String message = cause.getMessage();
        return truncate(cause.getClass().getSimpleName() + (message == null ? "" : ": " + message));
    }

    private String truncate(String value) {
        if (value == null) return "Unknown error";
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
