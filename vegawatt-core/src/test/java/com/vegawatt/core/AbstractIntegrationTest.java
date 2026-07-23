package com.vegawatt.core;

import org.apache.ignite.client.IgniteClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the whole core context against a real Postgres and a real Kafka broker. This is the first
 * test in the module to build a Spring context at all, so it is where a mistake that only surfaces
 * at startup finally gets caught: a Flyway migration that will not apply, an entity that no longer
 * matches the migrated schema under ddl-auto=validate, or a {@code @Scheduled} that names a
 * scheduler bean which does not exist.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    // I first reached for Redpanda here because its arm64 image starts fast on this machine, but the
    // whole class skips on this machine (its Docker is too new for the Testcontainers probe), so that
    // only ever mattered on CI, where the runner is amd64 anyway. On the runner Redpanda's advertised
    // listener did not line up with the mapped port and every Kafka client failed to connect, so I
    // moved to the Confluent image, whose Testcontainers listener wiring is the most exercised one in
    // the Spring ecosystem. The relay uses only the standard producer API, so the broker is
    // interchangeable as far as this test is concerned.
    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    // I mock the Ignite thin client rather than stand up a node. The live-state path it drives was
    // verified end to end against the real stack, and what these tests add is context wiring and the
    // outbox-to-Kafka relay, neither of which touches Ignite; a real node would also fight the
    // running compose stack for its fixed ports. The adapters call getOrCreateCache in their
    // constructors and get a null cache back, which is harmless here because no test invokes an
    // adapter method and startup reconciliation processes zero homes against a fresh database.
    @MockBean
    IgniteClient igniteClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // application.yml gives this no default, so the context will not start without it. The value
        // only needs to be long enough for Keys.hmacShaKeyFor to pick an HMAC-SHA variant.
        registry.add("vegawatt.jwt.secret",
                () -> "integration-test-signing-secret-of-at-least-256-bits-long");
    }
}
