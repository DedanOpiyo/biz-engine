package com.bizengine.insight.api;

import com.bizengine.insight.domain.EvidenceQuality;
import com.bizengine.insight.domain.InsightEvidence;
import com.bizengine.insight.domain.InsightRecord;
import com.bizengine.insight.domain.InsightSourceEvidence;
import com.bizengine.insight.domain.InsightStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InsightResponse(
        UUID id,
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
        List<EvidenceResponse> evidence,
        List<SourceEvidenceResponse> sourceEvidence) {

    public static InsightResponse from(InsightRecord insight) {
        return new InsightResponse(insight.id(), insight.scopeType(), insight.scopeId(), insight.signalCode(),
                insight.windowHours(), insight.ruleCode(), insight.ruleVersion(), insight.title(), insight.summary(),
                insight.direction(), insight.confidence(), insight.evidenceQuality(), insight.status(),
                insight.firstObservedAt(), insight.lastEvaluatedAt(), insight.expiresAt(),
                insight.evidence().stream().map(EvidenceResponse::from).toList(),
                insight.sourceEvidence().stream().map(SourceEvidenceResponse::from).toList());
    }

    public record SourceEvidenceResponse(
            UUID signalId,
            UUID observationId,
            UUID eventId,
            String signalCode,
            Instant observedAt,
            BigDecimal baseStrength) {
        static SourceEvidenceResponse from(InsightSourceEvidence evidence) {
            return new SourceEvidenceResponse(evidence.signalId(), evidence.observationId(), evidence.eventId(),
                    evidence.signalCode(), evidence.observedAt(), evidence.baseStrength());
        }
    }

    public record EvidenceResponse(
            UUID trendSnapshotId,
            String signalCode,
            int windowHours,
            Instant snapshotAt,
            long currentSignalCount,
            long previousSignalCount,
            long currentDistinctEntities,
            String direction,
            String role) {
        static EvidenceResponse from(InsightEvidence evidence) {
            return new EvidenceResponse(evidence.trendSnapshotId(), evidence.signalCode(), evidence.windowHours(),
                    evidence.snapshotAt(), evidence.currentSignalCount(), evidence.previousSignalCount(),
                    evidence.currentDistinctEntities(), evidence.direction(), evidence.role());
        }
    }
}
