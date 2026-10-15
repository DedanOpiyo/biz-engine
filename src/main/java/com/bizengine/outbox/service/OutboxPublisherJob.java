package com.bizengine.outbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "biz.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherJob {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private final OutboxDispatchService dispatchService;

    public OutboxPublisherJob(OutboxDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelayString = "${biz.outbox.poll-delay-ms:1000}")
    public void publishPendingMessages() {
        try {
            dispatchService.publishBatch();
        } catch (Exception exception) {
            // Keep the scheduler alive; pending rows remain durable and will be retried.
            log.error("Outbox batch failed; pending messages will be retried", exception);
        }
    }
}
