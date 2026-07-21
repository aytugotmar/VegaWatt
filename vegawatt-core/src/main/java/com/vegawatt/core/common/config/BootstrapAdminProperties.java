package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password) {
}
