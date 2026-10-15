package com.bizengine.signal.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Computes time-decayed strength without mutating the stored source signal. */
@Service
public class SignalRecencyService {
    public double currentWeight(double baseStrength, Instant observedAt, int halfLifeHours, Instant asOf) {
        if (baseStrength < 0 || baseStrength > 1) throw new IllegalArgumentException("baseStrength must be between 0 and 1");
        if (halfLifeHours <= 0) throw new IllegalArgumentException("halfLifeHours must be positive");
        if (observedAt == null || asOf == null) throw new IllegalArgumentException("timestamps are required");
        double elapsedHours = Math.max(0d, Duration.between(observedAt, asOf).toMillis() / 3_600_000d);
        return baseStrength * Math.pow(0.5d, elapsedHours / halfLifeHours);
    }
}
