package com.bizengine.event.service;

import com.bizengine.authorization.domain.AuthorizationPermission;
import com.bizengine.authorization.domain.AuthorizationScopeType;
import com.bizengine.authorization.service.PartnerAuthorizationService;
import com.bizengine.event.api.IngestEventRequest;
import com.bizengine.event.domain.OperationalEvent;
import com.bizengine.event.repository.OperationalEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventIngestionServiceTest {
    private PartnerAuthorizationService authorization;
    private OperationalEventRepository repository;
    private JdbcTemplate jdbcTemplate;
    private EventIngestionService service;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        authorization = mock(PartnerAuthorizationService.class);
        repository = mock(OperationalEventRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        mapper = new ObjectMapper().findAndRegisterModules();
        service = new EventIngestionService(authorization, repository, jdbcTemplate, mapper);
    }

    @Test
    void rejectsMissingAuthenticatedPartner() {
        IngestEventRequest request = request();
        assertThrows(com.bizengine.authorization.service.AuthorizationDeniedException.class,
                () -> service.ingest(null, "idempotency-key-0001", request));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void requiresExactApplicationGrantBeforeInsert() {
        UUID partnerId = UUID.randomUUID();
        IngestEventRequest request = request();
        org.mockito.Mockito.doReturn(java.util.List.of(UUID.randomUUID())).when(jdbcTemplate)
                .query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class));
        service.ingest(partnerId, "idempotency-key-0001", request);
        verify(authorization).requireExactScopePermission(partnerId, partnerId, AuthorizationPermission.EVENT_INGEST,
                AuthorizationScopeType.APPLICATION, request.applicationId().toString());
    }

    @Test
    void rejectsReusedKeyWithDifferentBody() {
        UUID partnerId = UUID.randomUUID();
        IngestEventRequest request = request();
        org.mockito.Mockito.doReturn(java.util.List.of()).when(jdbcTemplate)
                .query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class));
        OperationalEvent old = mock(OperationalEvent.class);
        when(repository.findByPartnerIdAndIdempotencyKey(partnerId, "idempotency-key-0001")).thenReturn(Optional.of(old));
        when(old.getRequestHash()).thenReturn("different-hash");
        assertThrows(EventIdempotencyConflictException.class,
                () -> service.ingest(partnerId, "idempotency-key-0001", request));
    }

    private IngestEventRequest request() {
        return new IngestEventRequest(UUID.randomUUID(), "content.viewed", 1, Instant.parse("2026-10-12T10:00:00Z"),
                "content", "content-123", mapper.createObjectNode(), mapper.createObjectNode().put("durationSeconds", 8));
    }
}
