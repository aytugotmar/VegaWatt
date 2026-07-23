package com.vegawatt.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * One scheduler per background job, rather than the single shared thread Spring Boot gives you
 * by default.
 *
 * <p>Without this, {@code spring.task.scheduling.pool.size} defaults to 1 and the outbox relay,
 * the notification worker and the snapshot capture all queue behind each other on one thread.
 * That coupling is not theoretical: the notification worker walks a batch of up to fifty jobs,
 * each of which can spend the full Gemini read timeout waiting, so a slow model can hold the
 * only scheduler thread for minutes. While it does, no outbox events are relayed, which means a
 * newly registered home never reaches Kafka, which means the sensors never learn about it and
 * produce no telemetry at all. A slow model should not be able to stop home registration, and
 * one shared thread is the only reason it can.
 *
 * <p>Each pool holds a single thread on purpose. Every job uses fixedDelay, so none can overlap
 * itself, and isolation between jobs is what is being bought here, not parallelism within one.
 *
 * <p>Because several TaskScheduler beans exist and none is primary, a future {@code @Scheduled}
 * that does not name one will not share these: Spring falls back to a private single-threaded
 * executor for it. Survivable, but silent, so name a scheduler explicitly on anything new.
 */
@Configuration
@EnableScheduling
class SchedulingConfig {

    @Bean
    TaskScheduler outboxTaskScheduler() {
        return newScheduler("outbox-relay-");
    }

    @Bean
    TaskScheduler notificationTaskScheduler() {
        return newScheduler("notification-worker-");
    }

    @Bean
    TaskScheduler snapshotTaskScheduler() {
        return newScheduler("snapshot-capture-");
    }

    private static ThreadPoolTaskScheduler newScheduler(String threadNamePrefix) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        // An outbox relay interrupted mid-send would leave a row that may or may not have
        // reached Kafka, so let an in-flight run finish before the context closes.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        scheduler.initialize();
        return scheduler;
    }
}
