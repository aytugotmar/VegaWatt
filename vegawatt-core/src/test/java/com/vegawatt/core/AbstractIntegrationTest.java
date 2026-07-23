package com.vegawatt.core;

import org.apache.ignite.client.IgniteClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
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

    // Redpanda speaks the Kafka protocol and ships a native arm64 image, so it is up in a couple of
    // seconds where the emulated Confluent image takes most of a minute on this hardware. The relay
    // under test uses only the standard producer API, which Redpanda serves faithfully, so this buys
    // speed without changing what is verified. Swap in a Kafka image here if that ever stops holding.
    @Container
    static final RedpandaContainer KAFKA =
            new RedpandaContainer(DockerImageName.parse("redpandadata/redpanda:v24.1.2"));

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
