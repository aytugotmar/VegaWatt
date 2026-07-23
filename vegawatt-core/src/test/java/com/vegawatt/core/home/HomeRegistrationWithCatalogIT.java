package com.vegawatt.core.home;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.VegaWattCoreApplication;
import com.vegawatt.core.home.api.RegisterHomeRequest;
import com.vegawatt.core.home.api.RegisterHomeResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.client.IgniteClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the Aşama 3 catalog↔appliance-instance integration end to end against a real,
 * freshly-migrated Postgres: registering a home with a catalogItemId-backed appliance snapshots
 * the catalog values, omitted watt fields fall back to catalog defaults, and unknown/mismatched
 * catalog selections are rejected — all via the real {@code POST /api/v1/homes} endpoint.
 *
 * {@code COFFEE_MACHINE}'s seed UUID ({@code V15__seed_appliance_catalog.sql}) is fixed and
 * deterministic, so it's hardcoded here rather than looked up.
 */
@Testcontainers
@SpringBootTest(classes = VegaWattCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HomeRegistrationWithCatalogIT {

    private static final UUID COFFEE_MACHINE_CATALOG_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("vegawatt.jwt.secret", () -> "integration-test-signing-secret-key-32-bytes-minimum");
    }

    // RETURNS_DEEP_STUBS: the Ignite adapters (IgniteHomeLiveStateAdapter etc.) call
    // igniteClient.getOrCreateCache(...) eagerly in their constructor and keep the returned
    // ClientCache as a field; a plain mock returns null there, NPE-ing the first time home
    // registration tries to initialize live state. Deep stubs makes every such call return
    // another (no-op) mock instead.
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private IgniteClient igniteClient;

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;

    @BeforeEach
    void registerTestUser() {
        Map<String, String> registerRequest = Map.of(
                "email", "home-catalog-it-" + UUID.randomUUID() + "@example.com",
                "password", "password123");
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                new HttpEntity<>(registerRequest), new org.springframework.core.ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        accessToken = (String) response.getBody().get("accessToken");
    }

    private <T> ResponseEntity<T> postAuthenticated(RegisterHomeRequest request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange("/api/v1/homes", HttpMethod.POST, new HttpEntity<>(request, headers),
                responseType);
    }

    private static RegisterHomeRequest homeRequest(RegisterHomeRequest.ApplianceRequest... appliances) {
        return new RegisterHomeRequest("Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("2500"), new BigDecimal("2.10"), new BigDecimal("3.50"), List.of(appliances));
    }

    @Test
    void registersApplianceFromCatalogWithProvidedOverrides() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                new BigDecimal("1450"), new BigDecimal("650"), new BigDecimal("1350"), COFFEE_MACHINE_CATALOG_ID);

        ResponseEntity<RegisterHomeResponse> response = postAuthenticated(homeRequest(appliance),
                RegisterHomeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var registeredAppliance = response.getBody().appliances().get(0);
        assertThat(registeredAppliance.catalogItemId()).isEqualTo(COFFEE_MACHINE_CATALOG_ID);
        assertThat(registeredAppliance.safePowerLimitWatt()).isEqualByComparingTo("1450");
    }

    @Test
    void registersApplianceFromCatalogUsingDefaultsWhenWattFieldsOmitted() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                null, null, null, COFFEE_MACHINE_CATALOG_ID);

        ResponseEntity<RegisterHomeResponse> response = postAuthenticated(homeRequest(appliance),
                RegisterHomeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var registeredAppliance = response.getBody().appliances().get(0);
        assertThat(registeredAppliance.safePowerLimitWatt()).isEqualByComparingTo("1500");
        assertThat(registeredAppliance.simulationMinWatt()).isEqualByComparingTo("600");
        assertThat(registeredAppliance.simulationMaxWatt()).isEqualByComparingTo("1300");
    }

    @Test
    void rejectsUnknownCatalogItemId() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Hayalet Cihaz", "GHOST",
                null, null, null, UUID.randomUUID());

        ResponseEntity<String> response = postAuthenticated(homeRequest(appliance), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registersCustomApplianceWithoutCatalogItemId() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Özel Cihaz", "CUSTOM_DEVICE",
                new BigDecimal("500"), new BigDecimal("50"), new BigDecimal("450"), null);

        ResponseEntity<RegisterHomeResponse> response = postAuthenticated(homeRequest(appliance),
                RegisterHomeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().appliances().get(0).catalogItemId()).isNull();
    }

    @Test
    void rejectsApplianceWithoutCatalogItemIdAndWithoutPowerRange() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Eksik Cihaz", "CUSTOM_DEVICE",
                null, null, null, null);

        ResponseEntity<String> response = postAuthenticated(homeRequest(appliance), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
