package com.vegawatt.core;

import org.apache.ignite.client.IgniteClient;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the whole core context against a real Postgres and a real Kafka broker.
 * The static containers are started explicitly in the static block to guarantee
 * that both PostgreSQL and Redpanda are fully up and listening before Spring Boot
 * context initialization or HikariCP DataSource creation begins.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vegawatt")
            .withUsername("vegawatt")
            .withPassword("vegawatt");

    @Container
    static final RedpandaContainer KAFKA =
            new RedpandaContainer(DockerImageName.parse("redpandadata/redpanda:v24.1.2"));

    @MockBean
    IgniteClient igniteClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("vegawatt.jwt.secret",
                () -> "integration-test-signing-secret-of-at-least-256-bits-long");
    }
}
