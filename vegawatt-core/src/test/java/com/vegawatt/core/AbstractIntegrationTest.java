package com.vegawatt.core;

import org.apache.ignite.client.IgniteClient;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the whole core context against a real Postgres and a real Kafka broker.
 *
 * <p>The containers are singletons: started once in the static block below and never stopped
 * between test classes. That lifecycle is the point. With {@code @Container} the Testcontainers
 * extension stops static containers in each class's afterAll, so once more than one integration
 * test class extended this base, whichever ran second reused a Spring context (cached, because the
 * configuration is identical) whose Hikari pool still pointed at the first class's already-stopped
 * container, and every query came back "connection refused". Starting them here and leaving them to
 * Ryuk at JVM exit keeps every context pointed at a live broker and database.
 *
 * <p>Confluent rather than Redpanda for Kafka: its Testcontainers listener wiring is the most
 * exercised one in the Spring ecosystem, and the relay only needs the standard producer API, so the
 * broker is interchangeable as far as these tests are concerned.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vegawatt")
            .withUsername("vegawatt")
            .withPassword("vegawatt");

    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        // The @Testcontainers(disabledWithoutDocker) condition skips these tests when Docker is
        // absent, but this static block runs at class-load regardless, so it makes the same check or
        // it would try to start a container on a machine that has none.
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
            KAFKA.start();
        }
    }

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
        // No need to mute the notification worker's interval any more: background scheduling is off
        // in the test profile (vegawatt.background-jobs.enabled=false), so no worker polls claimDue on
        // a timer and nothing races NotificationJobClaimIT for the jobs it inserts. Tests that need a
        // scheduled job to run call its method directly instead.
    }
}
