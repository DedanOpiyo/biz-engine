package com.bizengine.insight.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InsightRecord(
        UUID id,
        UUID partnerId,
        String scopeType,
        UUID scopeId,
        String signalCode,
        int windowHours,
        String ruleCode,
        int ruleVersion,
        String title,
        String summary,
        String direction,
        BigDecimal confidence,
        EvidenceQuality evidenceQuality,
        InsightStatus status,
        Instant firstObservedAt,
        Instant lastEvaluatedAt,
        Instant expiresAt,
        List<InsightEvidence> evidence,
        List<InsightSourceEvidence> sourceEvidence) {
}
