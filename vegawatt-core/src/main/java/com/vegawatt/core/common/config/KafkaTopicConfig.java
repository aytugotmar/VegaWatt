package com.vegawatt.core.common.config;

import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    private static final String DEAD_LETTER_SUFFIX = ".DLT";
    private static final int REGISTRATION_PARTITIONS = 3;
    private static final int TELEMETRY_PARTITIONS = 6;

    @Bean
    public NewTopic registrationTopic(VegaWattKafkaProperties kafkaProperties) {
        return new NewTopic(kafkaProperties.registrationTopic(), REGISTRATION_PARTITIONS, (short) 1)
                .configs(Map.of(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT));
    }

    @Bean
    public NewTopic telemetryTopic(VegaWattKafkaProperties kafkaProperties) {
        return new NewTopic(kafkaProperties.telemetryTopic(), TELEMETRY_PARTITIONS, (short) 1)
                .configs(Map.of(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE));
    }

    @Bean
    public NewTopic telemetryDeadLetterTopic(VegaWattKafkaProperties kafkaProperties) {
        return new NewTopic(kafkaProperties.telemetryTopic() + DEAD_LETTER_SUFFIX, TELEMETRY_PARTITIONS, (short) 1)
                .configs(Map.of(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE));
    }
}
