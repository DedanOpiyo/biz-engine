package com.bizengine.insight.service;

import com.bizengine.insight.domain.EvidenceQuality;
import com.bizengine.insight.repository.InsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class InsightGenerationService {
    private static final String INCREASE_RULE = "SIGNAL_ACTIVITY_INCREASE";
    private static final String DECREASE_RULE = "SIGNAL_ACTIVITY_DECREASE";
    private final InsightRepository repository;
    private final InsightConfidenceCalculator confidenceCalculator;

    public InsightGenerationService(InsightRepository repository, InsightConfidenceCalculator confidenceCalculator) {
        this.repository = repository;
        this.confidenceCalculator = confidenceCalculator;
    }

    @Transactional
    public int generateRecent(Instant now) {
        int generated = 0;
        // Re-evaluate a bounded lookback. Uniqueness keys make repeated scheduled runs idempotent.
        for (InsightRepository.Candidate candidate : repository.findRecentCandidates(now.minus(Duration.ofDays(8)), 2000)) {
            var assessment = confidenceCalculator.assess(candidate.currentCount(), candidate.distinctEntities(), candidate.previousCount());
            boolean increase = "UP".equals(candidate.direction());
            String ruleCode = increase ? INCREASE_RULE : DECREASE_RULE;
            String signalLabel = candidate.signalCode().toLowerCase(Locale.ROOT).replace('_', ' ').replace('.', ' ');
            String title = increase ? "Increased " + signalLabel + " activity" : "Decreased " + signalLabel + " activity";
            String summary = String.format(Locale.ROOT,
                    "During the %d-hour window, %s: %d signals across %d distinct entities; the previous equal window recorded %d. This is a descriptive pattern, not evidence of its cause.",
                    candidate.windowHours(), candidate.direction().equals("UP") ? "activity increased" : "activity decreased",
                    candidate.currentCount(), candidate.distinctEntities(), candidate.previousCount());
            Instant expiry = candidate.snapshotAt().plus(Duration.ofHours(Math.max(24, candidate.windowHours())));
            repository.upsert(candidate, ruleCode, 1, title, summary, assessment.score(), assessment.quality(), expiry);
            generated++;
        }
        repository.expire(now);
        return generated;
    }
}
