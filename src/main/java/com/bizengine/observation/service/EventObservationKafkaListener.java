package com.bizengine.observation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventObservationKafkaListener {
    private final ObjectMapper objectMapper;
    private final ObservationProcessingService processingService;

    public EventObservationKafkaListener(ObjectMapper objectMapper, ObservationProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @KafkaListener(topics = "${biz.outbox.topic:biz.events.v1}",
            groupId = "${biz.observation.consumer-group:biz-observation-v1}")
    public void consume(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            processingService.process(envelope);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not process Biz-Engine event envelope.", ex);
        }
    }
}
