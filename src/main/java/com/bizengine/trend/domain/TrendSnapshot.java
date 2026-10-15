package com.bizengine.trend.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrendSnapshot(
        UUID id,
        String scopeType,
        UUID scopeId,
        String signalCode,
        int windowHours,
        Instant snapshotAt,
        long currentSignalCount,
        long previousSignalCount,
        long currentDistinctEntities,
        BigDecimal currentWeightedStrength,
        BigDecimal previousWeightedStrength,
        BigDecimal changeRatio,
        TrendDirection direction,
        int algorithmVersion) {
}
