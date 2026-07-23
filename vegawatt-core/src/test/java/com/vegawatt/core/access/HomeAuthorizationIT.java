package com.vegawatt.core.access;

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
import org.springframework.core.ParameterizedTypeReference;
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
 * Guards against the home-scoped IDOR: a user who isn't a member of a home must not be able to
 * read its history, recommendations, or events by guessing/knowing its UUID (Yol Haritası §1.1).
 */
@Testcontainers
@SpringBootTest(classes = VegaWattCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HomeAuthorizationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("vegawatt.jwt.secret", () -> "integration-test-signing-secret-key-32-bytes-minimum");
    }

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private IgniteClient igniteClient;

    @Autowired
    private TestRestTemplate restTemplate;

    private String ownerAccessToken;
    private String strangerAccessToken;
    private UUID homeId;

    @BeforeEach
    void registerOwnerWithHomeAndAStranger() {
        ownerAccessToken = registerUser("owner");
        strangerAccessToken = registerUser("stranger");

        var appliance = new RegisterHomeRequest.ApplianceRequest("Salon Lambası", "CUSTOM_DEVICE",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"), null);
        var request = new RegisterHomeRequest("Yetki Testi Evi", "owner@example.com", new BigDecimal("500"),
                new BigDecimal("2500"), new BigDecimal("2.10"), new BigDecimal("3.50"), List.of(appliance));

        ResponseEntity<RegisterHomeResponse> response = restTemplate.exchange("/api/v1/homes", HttpMethod.POST,
                authenticated(request, ownerAccessToken), RegisterHomeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        homeId = response.getBody().homeId();
    }

    @Test
    void ownerCanReadHistoryRecommendationsAndEvents() {
        assertThat(get("/api/v1/homes/" + homeId + "/history", ownerAccessToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/homes/" + homeId + "/recommendations", ownerAccessToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/homes/" + homeId + "/events", ownerAccessToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void strangerCannotReadHistoryRecommendationsOrEventsOfSomeoneElsesHome() {
        assertThat(get("/api/v1/homes/" + homeId + "/history", strangerAccessToken).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/v1/homes/" + homeId + "/recommendations", strangerAccessToken).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/v1/homes/" + homeId + "/events", strangerAccessToken).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> get(String path, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private <T> HttpEntity<T> authenticated(T body, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }

    private String registerUser(String label) {
        Map<String, String> registerRequest = Map.of(
                "email", "home-auth-it-" + label + "-" + UUID.randomUUID() + "@example.com",
                "password", "password123");
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                new HttpEntity<>(registerRequest), new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("accessToken");
    }
}
