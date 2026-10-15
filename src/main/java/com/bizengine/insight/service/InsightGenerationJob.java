package com.bizengine.insight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "biz.insight.generation.enabled", havingValue = "true", matchIfMissing = true)
public class InsightGenerationJob {
    private static final Logger log = LoggerFactory.getLogger(InsightGenerationJob.class);
    private final InsightGenerationService service;

    public InsightGenerationJob(InsightGenerationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${biz.insight.generation.fixed-delay-ms:900000}")
    public void generate() {
        int count = service.generateRecent(Instant.now());
        log.info("Insight generation completed: evaluatedCandidates={}", count);
    }
}
