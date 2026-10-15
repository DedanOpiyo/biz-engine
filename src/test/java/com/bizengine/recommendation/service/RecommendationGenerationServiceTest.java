package com.bizengine.recommendation.service;

import com.bizengine.recommendation.domain.RecommendationPriority;
import com.bizengine.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationGenerationServiceTest {
    @Mock RecommendationRepository repository;
    @InjectMocks RecommendationGenerationService service;

    @Test
    void generatesReviewPromptFromEligibleInsightWithoutExecutingAction() {
        Instant now = Instant.parse("2026-10-15T07:00:00Z");
        var insight = new RecommendationRepository.InsightCandidate(
                UUID.randomUUID(), UUID.randomUUID(), "content.viewed", "UP",
                "Increased content viewed activity", "Activity increased in the current window.",
                new BigDecimal("0.8500"), "HIGH", now.plusSeconds(86400));
        when(repository.findEligibleInsights(now, 1000)).thenReturn(List.of(insight));

        int count = service.generateRecent(now);

        assertEquals(1, count);
        ArgumentCaptor<RecommendationPriority> priority = ArgumentCaptor.forClass(RecommendationPriority.class);
        verify(repository).upsert(eq(insight), eq("REVIEW_ACTIVITY_INCREASE"), eq("UNDERSTAND_ACTIVITY_CHANGE"),
                contains("Review increased"), contains("Activity increased"), contains("Review the relevant"),
                priority.capture(), eq(insight.expiresAt()));
        assertEquals(RecommendationPriority.HIGH, priority.getValue());
        verify(repository).expire(now);
        // The service creates a suggestion record only; it has no action-execution dependency.
        verifyNoMoreInteractions(repository);
    }
}
