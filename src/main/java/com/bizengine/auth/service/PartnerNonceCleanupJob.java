package com.bizengine.auth.service;

import com.bizengine.auth.repository.PartnerRequestNonceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class PartnerNonceCleanupJob {
    private final PartnerRequestNonceRepository nonceRepository;

    public PartnerNonceCleanupJob(PartnerRequestNonceRepository nonceRepository) {
        this.nonceRepository = nonceRepository;
    }

    @Scheduled(fixedDelayString = "${biz.security.partner-authentication.nonce-cleanup-delay-ms:60000}")
    @Transactional
    public void removeExpiredNonces() {
        nonceRepository.deleteByCreatedAtBefore(Instant.now().minus(15, ChronoUnit.MINUTES));
    }
}
