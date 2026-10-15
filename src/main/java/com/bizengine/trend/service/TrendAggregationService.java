package com.bizengine.trend.service;

import com.bizengine.trend.repository.TrendRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TrendAggregationService {
    public static final List<Integer> SUPPORTED_WINDOWS_HOURS = List.of(1, 24, 168, 720);
    private final TrendRepository repository;

    public TrendAggregationService(TrendRepository repository) {
        this.repository = repository;
    }

    public int aggregateWindow(Instant snapshotAt, int windowHours) {
        if (!SUPPORTED_WINDOWS_HOURS.contains(windowHours)) {
            throw new IllegalArgumentException("Unsupported trend window. Supported values: 1, 24, 168, 720 hours.");
        }
        return repository.aggregate(snapshotAt, windowHours);
    }

    public int aggregateAll(Instant snapshotAt) {
        int total = 0;
        for (int window : SUPPORTED_WINDOWS_HOURS) {
            total += aggregateWindow(snapshotAt, window);
        }
        return total;
    }
}
