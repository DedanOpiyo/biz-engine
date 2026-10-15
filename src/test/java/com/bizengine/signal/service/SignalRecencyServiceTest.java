package com.bizengine.signal.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignalRecencyServiceTest {
    private final SignalRecencyService service = new SignalRecencyService();

    @Test
    void halvesWeightAfterOneHalfLife() {
        Instant observed = Instant.parse("2026-01-01T00:00:00Z");
        double result = service.currentWeight(0.8, observed, 24, observed.plusSeconds(24 * 3600L));
        assertEquals(0.4, result, 0.000001);
    }

    @Test
    void futureObservationDoesNotDecayYet() {
        Instant observed = Instant.parse("2026-01-02T00:00:00Z");
        assertEquals(0.8, service.currentWeight(0.8, observed, 24, observed.minusSeconds(3600)), 0.000001);
    }

    @Test
    void rejectsInvalidHalfLife() {
        assertThrows(IllegalArgumentException.class, () ->
                service.currentWeight(0.5, Instant.now(), 0, Instant.now()));
    }
}
