package com.bizengine.insight.service;

import com.bizengine.insight.domain.EvidenceQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsightConfidenceCalculatorTest {
    private final InsightConfidenceCalculator calculator = new InsightConfidenceCalculator();

    @Test
    void sparseEvidenceGetsLowQualityAndBoundedScore() {
        var result = calculator.assess(5, 3, 0);
        assertEquals(EvidenceQuality.LOW, result.quality());
        assertTrue(result.score().doubleValue() >= 0.0 && result.score().doubleValue() <= 0.95);
    }

    @Test
    void diverseRepeatedEvidenceCanReachHighQuality() {
        var result = calculator.assess(25, 12, 20);
        assertEquals(EvidenceQuality.HIGH, result.quality());
        assertEquals(0.95, result.score().doubleValue(), 0.0001);
    }

    @Test
    void negativeCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.assess(-1, 2, 0));
    }

    @Test
    void missingEvidenceHasZeroScore() {
        var result = calculator.assess(0, 0, 0);
        assertEquals(0.0, result.score().doubleValue());
        assertEquals(EvidenceQuality.LOW, result.quality());
    }
}
