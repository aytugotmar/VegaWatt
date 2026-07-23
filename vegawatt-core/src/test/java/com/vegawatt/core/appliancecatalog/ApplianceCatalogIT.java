package com.vegawatt.core.appliancecatalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.VegaWattCoreApplication;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.client.IgniteClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots the real application against a fresh Testcontainers Postgres so Flyway applies V1..V15 for
 * real — this is what actually proves the appliance_catalog migration/seed are valid (a Mockito
 * unit test can't catch a bad SQL statement or a violated CHECK constraint). Ignite is mocked out
 * ({@link IgniteClient} fails fast if no server is reachable) since nothing under test here touches
 * live-state; Kafka is left as-is since Spring Kafka tolerates a missing broker at startup.
 */
@Testcontainers
@SpringBootTest(classes = VegaWattCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplianceCatalogIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("vegawatt.jwt.secret", () -> "integration-test-signing-secret-key-32-bytes-minimum");
    }

    @MockBean
    private IgniteClient igniteClient;

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;

    private record CatalogItemDto(String code, String category, boolean featured) {
    }

    @BeforeEach
    void registerTestUser() {
        Map<String, String> registerRequest = Map.of(
                "email", "catalog-it-" + UUID.randomUUID() + "@example.com",
                "password", "password123");
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                new HttpEntity<>(registerRequest), new org.springframework.core.ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        accessToken = (String) response.getBody().get("accessToken");
    }

    private HttpEntity<Void> authenticated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    @Test
    void listReturnsAllFortyFiveSeededItemsWithUniqueCodes() {
        ResponseEntity<CatalogItemDto[]> response = restTemplate.exchange("/api/v1/appliance-catalog",
                HttpMethod.GET, authenticated(), CatalogItemDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<CatalogItemDto> items = List.of(response.getBody());
        assertThat(items).hasSize(45);
        assertThat(items).extracting(CatalogItemDto::code).doesNotHaveDuplicates();
    }

    @Test
    void listFiltersByCategory() {
        ResponseEntity<CatalogItemDto[]> response = restTemplate.exchange(
                "/api/v1/appliance-catalog?category=KITCHEN", HttpMethod.GET, authenticated(), CatalogItemDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<CatalogItemDto> items = List.of(response.getBody());
        assertThat(items).hasSize(10);
        assertThat(items).allMatch(item -> "KITCHEN".equals(item.category()));
    }

    @Test
    void listFiltersBySearchWithTurkishCharacters() {
        ResponseEntity<CatalogItemDto[]> response = restTemplate.exchange(
                "/api/v1/appliance-catalog?search=kahve", HttpMethod.GET, authenticated(), CatalogItemDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(response.getBody())).extracting(CatalogItemDto::code).containsExactly("COFFEE_MACHINE");
    }

    @Test
    void listFiltersByFeatured() {
        ResponseEntity<CatalogItemDto[]> response = restTemplate.exchange(
                "/api/v1/appliance-catalog?featured=true", HttpMethod.GET, authenticated(), CatalogItemDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<CatalogItemDto> items = List.of(response.getBody());
        assertThat(items).hasSize(13);
        assertThat(items).allMatch(CatalogItemDto::featured);
    }

    @Test
    void detailReturnsItemForKnownCode() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/appliance-catalog/COFFEE_MACHINE", HttpMethod.GET, authenticated(),
                new org.springframework.core.ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("code")).isEqualTo("COFFEE_MACHINE");
        // COFFEE_MACHINE is SHORT_HIGH_POWER; spec §19 also lists SESSION_DURATION_EXCEEDED for
        // that profile, but it's not implemented yet so it must not surface.
        assertThat(response.getBody()).containsEntry("supportedTriggers",
                List.of("SAFE_POWER_LIMIT_BREACHED", "TELEMETRY_STALE"));
    }

    @Test
    void detailReturnsNotFoundForUnknownCode() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/appliance-catalog/NOT_A_REAL_CODE", HttpMethod.GET, authenticated(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listRejectsUnauthenticatedRequest() {
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/appliance-catalog", HttpMethod.GET,
                HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
