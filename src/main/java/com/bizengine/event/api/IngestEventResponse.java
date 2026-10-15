package com.bizengine.event.api;

import java.time.Instant;
import java.util.UUID;

public record IngestEventResponse(UUID eventId, UUID applicationId, String eventType,
                                  Instant occurredAt, Instant receivedAt, boolean duplicate) { }
