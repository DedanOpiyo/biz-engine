package com.bizengine.outbox.domain;

import java.util.UUID;

public record OutboxMessage(UUID id, UUID aggregateId, String eventType, String topic,
                            String messageKey, String payload, int attempts) { }
