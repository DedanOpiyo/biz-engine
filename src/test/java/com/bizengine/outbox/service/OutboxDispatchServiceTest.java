package com.bizengine.outbox.service;

import com.bizengine.outbox.domain.OutboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxDispatchServiceTest {
    private JdbcTemplate jdbc;
    private KafkaTemplate<String, String> kafka;
    private OutboxDispatchService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        kafka = mock(KafkaTemplate.class);
        service = new OutboxDispatchService(jdbc, kafka, new ObjectMapper(), 50, 100, 5, 10, 1000,
                "biz.events.dlq");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void returnsZeroWhenNoPendingMessages() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(50))).thenReturn(List.of());
        assertEquals(0, service.publishBatch());
        verifyNoInteractions(kafka);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void marksMessagePublishedOnlyAfterKafkaAcknowledges() {
        UUID outboxId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        OutboxMessage message = new OutboxMessage(outboxId, eventId, "biz.event.accepted.v1",
                "biz.events.v1", "application-1", "{\"eventId\":\"" + eventId + "\"}", 0);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(50))).thenReturn(List.of(message));
        when(kafka.send("biz.events.v1", "application-1", message.payload()))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertEquals(1, service.publishBatch());
        verify(jdbc).update(contains("status = 'PUBLISHED'"), any(), eq(outboxId));
    }
}
