package com.vegawatt.core.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI vegaWattOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VegaWatt Core API")
                .description("Home registration, live status, billing history and energy-saving recommendations")
                .version("v1"));
    }
}
