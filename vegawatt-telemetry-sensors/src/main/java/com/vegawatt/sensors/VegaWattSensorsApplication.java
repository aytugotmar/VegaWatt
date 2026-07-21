package com.vegawatt.sensors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VegaWattSensorsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VegaWattSensorsApplication.class, args);
    }
}
