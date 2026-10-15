package com.bizengine.signal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SignalRecord(
        UUID id,
        UUID observationId,
        UUID evidenceEventId,
        UUID partnerId,
        UUID applicationId,
        UUID entityId,
        SignalScopeType scopeType,
        UUID scopeId,
        String signalCode,
        String ruleCode,
        int ruleVersion,
        BigDecimal baseStrength,
        int halfLifeHours,
        Instant observedAt,
        Instant expiresAt) {
}
