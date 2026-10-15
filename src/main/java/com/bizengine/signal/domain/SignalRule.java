package com.bizengine.signal.domain;

import java.math.BigDecimal;

public record SignalRule(
        String ruleCode,
        int ruleVersion,
        String eventType,
        String signalCode,
        BigDecimal baseStrength,
        int halfLifeHours,
        int ttlHours) {
}
