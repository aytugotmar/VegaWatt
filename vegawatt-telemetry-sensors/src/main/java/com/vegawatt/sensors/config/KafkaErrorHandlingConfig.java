package com.vegawatt.sensors.config;

import com.vegawatt.sensors.registration.MalformedRegistrationEventException;
import com.vegawatt.sensors.registration.UnsupportedRegistrationEventVersionException;
import org.apache.kafka.clients.admin.NewTopic;
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
 * Mirrors vegawatt-core's KafkaErrorHandlingConfig: malformed/unsupported-version registration
 * events are permanent failures and go straight to the dead-letter topic instead of vanishing
 * after a single log line; anything else is retried with backoff first.
 */
@Configuration
class KafkaErrorHandlingConfig {

    private static final String DEAD_LETTER_SUFFIX = ".DLT";

    // Must match vegawatt-core's KafkaTopicConfig.REGISTRATION_PARTITIONS. DeadLetterPublishingRecoverer's
    // default resolver below preserves the source record's partition number on the .DLT topic — if that
    // topic gets auto-created with the broker's default (often 1) instead of provisioned explicitly here,
    // any dead-lettered record from a source partition >= 1 fails to publish. Provisioned by this module
    // (not vegawatt-core, which never produces to this topic) since docker-compose lets
    // vegawatt-telemetry-sensors start before vegawatt-core — it can't depend on core's topic bean
    // having run first.
    private static final int REGISTRATION_PARTITIONS = 3;

    @Bean
    NewTopic registrationDeadLetterTopic(SensorsKafkaProperties kafkaProperties) {
        return new NewTopic(kafkaProperties.registrationTopic() + DEAD_LETTER_SUFFIX, REGISTRATION_PARTITIONS,
                (short) 1);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
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
        errorHandler.addNotRetryableExceptions(MalformedRegistrationEventException.class,
                UnsupportedRegistrationEventVersionException.class);
        return errorHandler;
    }
}
