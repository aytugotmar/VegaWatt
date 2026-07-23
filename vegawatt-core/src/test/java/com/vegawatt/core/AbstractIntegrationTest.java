package com.vegawatt.core;

import org.apache.ignite.client.IgniteClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
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

    // These are singleton containers, started once here and never stopped between test classes, and
    // that lifecycle is the point. With @Container the Testcontainers extension stops static
    // containers in each class's afterAll, so once there was a second integration test class the one
    // that ran second reused a Spring context (cached, because the configuration is identical) whose
    // Hikari pool still pointed at the first class's already-stopped container, and every query came
    // back "connection refused". Starting them in a static block and leaving them to Ryuk at JVM exit
    // keeps every context pointed at a live broker and database.
    //
    // Confluent rather than Redpanda for Kafka: its Testcontainers listener wiring is the most
    // exercised one in the Spring ecosystem, and the relay only needs the standard producer API, so
    // the broker is interchangeable as far as these tests are concerned.
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        // The @Testcontainers(disabledWithoutDocker) condition skips the tests when Docker is absent,
        // but this static block runs at class-load regardless, so it has to make the same check or it
        // would try to start a container on a machine that has none (this one, whose Docker is too new
        // for the Testcontainers probe).
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
            KAFKA.start();
        }
    }

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
        // The context's own notification worker polls with claimDue on a timer. Since these tests
        // share one context, that worker would race NotificationJobClaimIntegrationTest for the very
        // jobs it inserts and quietly claim some out from under it. Pushing its interval past any test
        // run leaves the test the only claimant, without touching the outbox relay the relay test
        // needs (that is a separate interval).
        registry.add("vegawatt.notification.worker-interval-ms", () -> "3600000");
    }
}
