package com.bizengine.trend.api;

import com.bizengine.trend.domain.TrendDirection;
import com.bizengine.trend.domain.TrendSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrendResponse(
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
    public static TrendResponse from(TrendSnapshot snapshot) {
        return new TrendResponse(snapshot.id(), snapshot.scopeType(), snapshot.scopeId(), snapshot.signalCode(),
                snapshot.windowHours(), snapshot.snapshotAt(), snapshot.currentSignalCount(),
                snapshot.previousSignalCount(), snapshot.currentDistinctEntities(), snapshot.currentWeightedStrength(),
                snapshot.previousWeightedStrength(), snapshot.changeRatio(), snapshot.direction(), snapshot.algorithmVersion());
    }
}
