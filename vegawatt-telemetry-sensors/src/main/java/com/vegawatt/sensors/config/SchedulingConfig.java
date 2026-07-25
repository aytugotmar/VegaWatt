package com.vegawatt.sensors.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
class SchedulingConfig {

    @Bean
    TaskScheduler taskScheduler(@Value("${vegawatt.simulation.scheduler-pool-size:10}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("telemetry-sim-");
        scheduler.initialize();
        return scheduler;
    }
}
