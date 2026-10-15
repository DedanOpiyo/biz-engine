package com.bizengine.recommendation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RecommendationGenerationJob {
    private static final Logger log = LoggerFactory.getLogger(RecommendationGenerationJob.class);
    private final RecommendationGenerationService service;

    public RecommendationGenerationJob(RecommendationGenerationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${biz-engine.recommendations.fixed-delay-ms:900000}")
    public void generate() {
        int count = service.generateRecent(Instant.now());
        if (count > 0) {
            log.info("Recommendation generation evaluated {} eligible insight(s)", count);
        }
    }
}
