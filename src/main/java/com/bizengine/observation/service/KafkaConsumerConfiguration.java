package com.bizengine.observation.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfiguration {
    @Bean
    public CommonErrorHandler bizEventConsumerErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
                                                           @Value("${biz.observation.dlq-suffix:.DLQ}") String dlqSuffix,
                                                           @Value("${biz.observation.retry-delay-ms:1000}") long retryDelayMs,
                                                           @Value("${biz.observation.retry-count:3}") long retryCount) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + dlqSuffix, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(Math.max(0, retryDelayMs), Math.max(0, retryCount)));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, CommonErrorHandler bizEventConsumerErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(bizEventConsumerErrorHandler);
        return factory;
    }
}
