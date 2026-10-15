package com.bizengine.recommendation.api;

import com.bizengine.recommendation.domain.RecommendationRecord;
import com.bizengine.recommendation.domain.RecommendationPriority;
import com.bizengine.recommendation.domain.RecommendationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecommendationResponse(
        UUID id,
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

    public static RecommendationResponse from(RecommendationRecord record) {
        return new RecommendationResponse(record.id(), record.sourceInsightId(), record.recommendationCode(),
                record.objective(), record.title(), record.rationale(), record.suggestedAction(), record.priority(),
                record.confidence(), record.evidenceQuality(), record.status(), record.validUntil(),
                record.createdAt(), record.updatedAt());
    }
}
