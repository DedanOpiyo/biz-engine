package com.bizengine.recommendation.service;

import com.bizengine.recommendation.domain.RecommendationPriority;
import com.bizengine.recommendation.repository.RecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class RecommendationGenerationService {
    private final RecommendationRepository repository;

    public RecommendationGenerationService(RecommendationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int generateRecent(Instant now) {
        int generated = 0;
        for (var insight : repository.findEligibleInsights(now, 1000)) {
            boolean increased = "UP".equals(insight.direction());
            String code = increased ? "REVIEW_ACTIVITY_INCREASE" : "REVIEW_ACTIVITY_DECREASE";
            String signal = insight.signalCode().toLowerCase(Locale.ROOT).replace('_', ' ').replace('.', ' ');
            String title = increased ? "Review increased " + signal + " activity" : "Investigate decreased " + signal + " activity";
            String rationale = insight.summary() + " This recommendation is a review prompt; it does not establish cause or authorize an action.";
            String action = increased
                    ? "Review the relevant product or content context and compare it with the previous period before changing strategy."
                    : "Check for data-quality issues and relevant product or content changes before deciding whether intervention is warranted.";
            RecommendationPriority priority = insight.confidence().compareTo(new java.math.BigDecimal("0.80")) >= 0
                    && "HIGH".equals(insight.evidenceQuality()) ? RecommendationPriority.HIGH
                    : insight.confidence().compareTo(new java.math.BigDecimal("0.65")) >= 0
                    ? RecommendationPriority.MEDIUM : RecommendationPriority.LOW;
            Instant validUntil = insight.expiresAt().isBefore(now.plus(Duration.ofDays(7)))
                    ? insight.expiresAt() : now.plus(Duration.ofDays(7));
            if (validUntil.isAfter(now)) {
                repository.upsert(insight, code, "UNDERSTAND_ACTIVITY_CHANGE", title, rationale, action, priority, validUntil);
                generated++;
            }
        }
        repository.expire(now);
        return generated;
    }
}
