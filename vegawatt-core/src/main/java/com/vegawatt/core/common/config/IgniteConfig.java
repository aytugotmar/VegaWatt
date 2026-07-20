package com.vegawatt.core.common.config;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IgniteConfig {

    @Bean(destroyMethod = "close")
    IgniteClient igniteClient(VegaWattIgniteProperties properties) {
        String[] addresses = properties.addresses().split(",");
        ClientConfiguration clientConfiguration = new ClientConfiguration().setAddresses(addresses);
        return Ignition.startClient(clientConfiguration);
    }
}
