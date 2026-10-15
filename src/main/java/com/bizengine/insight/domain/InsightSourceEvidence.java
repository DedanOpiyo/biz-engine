package com.bizengine.insight.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InsightSourceEvidence(
        UUID signalId,
        UUID observationId,
        UUID eventId,
        String signalCode,
        Instant observedAt,
        BigDecimal baseStrength) {
}
