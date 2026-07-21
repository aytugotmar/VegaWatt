package com.vegawatt.core.common.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class HttpClientConfig {

    @Bean
    HttpClient httpClient(GeminiProperties geminiProperties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(geminiProperties.connectTimeoutMs()))
                .build();
    }
}
