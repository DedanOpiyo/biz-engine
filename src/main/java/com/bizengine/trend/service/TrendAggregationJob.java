package com.bizengine.trend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "biz.trend.aggregation.enabled", havingValue = "true", matchIfMissing = true)
public class TrendAggregationJob {
    private static final Logger log = LoggerFactory.getLogger(TrendAggregationJob.class);
    private final TrendAggregationService service;

    public TrendAggregationJob(TrendAggregationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${biz.trend.aggregation.fixed-delay-ms:900000}")
    public void aggregate() {
        Instant snapshotAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        int affected = service.aggregateAll(snapshotAt);
        log.info("Trend aggregation completed: snapshotAt={}, affectedRows={}", snapshotAt, affected);
    }
}
