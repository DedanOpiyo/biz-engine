package com.bizengine.trend.service;

import com.bizengine.trend.repository.TrendRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendAggregationServiceTest {
    @Mock TrendRepository repository;
    @InjectMocks TrendAggregationService service;

    @Test
    void aggregatesSupportedWindow() {
        Instant snapshotAt = Instant.parse("2026-10-13T10:00:00Z");
        when(repository.aggregate(snapshotAt, 24)).thenReturn(3);
        assertEquals(3, service.aggregateWindow(snapshotAt, 24));
        verify(repository).aggregate(snapshotAt, 24);
    }

    @Test
    void rejectsUnsupportedWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.aggregateWindow(Instant.now(), 13));
    }
}
