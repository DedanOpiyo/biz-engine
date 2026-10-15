package com.bizengine.recommendation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecommendationRecord(
        UUID id,
        UUID partnerId,
        UUID sourceInsightId,
        String recommendationCode,
        String objective,
        String title,
        String rationale,
        String suggestedAction,
        RecommendationPriority priority,
        BigDecimal confidence,
        String evidenceQuality,
        RecommendationStatus status,
        Instant validUntil,
        Instant createdAt,
        Instant updatedAt) {
}
