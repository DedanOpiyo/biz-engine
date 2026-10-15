package com.bizengine.insight.domain;

import java.time.Instant;
import java.util.UUID;

public record InsightEvidence(
        UUID trendSnapshotId,
        String signalCode,
        int windowHours,
        Instant snapshotAt,
        long currentSignalCount,
        long previousSignalCount,
        long currentDistinctEntities,
        String direction,
        String role) {
}
