package com.bizengine.event.api;

import com.bizengine.auth.web.PartnerAuthenticationFilter;
import com.bizengine.event.service.EventIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final EventIngestionService ingestionService;

    public EventController(EventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<IngestEventResponse> ingest(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody IngestEventRequest request,
            HttpServletRequest servletRequest) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{16,128}")) {
            throw new com.bizengine.shared.error.ApiException(HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must contain 16–128 safe characters.");
        }
        Object partnerAttribute = servletRequest.getAttribute(PartnerAuthenticationFilter.AUTHENTICATED_PARTNER_ID_ATTRIBUTE);
        UUID partnerId = partnerAttribute instanceof UUID id ? id : null;
        EventIngestionService.IngestResult result = ingestionService.ingest(partnerId, idempotencyKey, request);
        IngestEventResponse response = new IngestEventResponse(result.eventId(), result.applicationId(),
                result.eventType(), result.occurredAt(), result.receivedAt(), result.duplicate());
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }
}
