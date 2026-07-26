package com.vegawatt.core.common.config;

import com.vegawatt.core.home.domain.ApplianceNotFoundException;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.telemetry.infrastructure.UnsupportedTelemetryEventVersionException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Malformed events and events referencing a missing home/appliance are permanent failures and go
 * straight to the dead-letter topic; everything else (transient Postgres/Ignite failures) is
 * retried with backoff first, per NFR 5.2.2.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    private static final String DEAD_LETTER_SUFFIX = ".DLT";

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));
        return factory;
    }

    private DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, record.partition()));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(HomeNotFoundException.class, ApplianceNotFoundException.class,
                IllegalArgumentException.class, UnsupportedTelemetryEventVersionException.class);
        return errorHandler;
    }
}
