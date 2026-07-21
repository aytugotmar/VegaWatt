package com.vegawatt.core.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.config.GeminiProperties;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AdvisoryResult;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GeminiAdvisoryClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private static final AdvisoryContext CONTEXT = new AdvisoryContext(
            UUID.randomUUID(), "Test Ev", AdvisoryTriggerType.QUOTA_80, new BigDecimal("85"),
            new BigDecimal("40"), Money.of(new BigDecimal("120.00")), TariffState.BASE, List.of(),
            UUID.randomUUID());

    @Test
    void returnsFallbackWhenNoApiKeyConfigured() {
        GeminiProperties properties = new GeminiProperties("", "gemini-2.0-flash", 3000, 8000);
        GeminiAdvisoryClient client = new GeminiAdvisoryClient(httpClient, objectMapper, properties);

        AdvisoryResult result = client.generateAdvisory(CONTEXT);

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.content()).isNotBlank();
    }

    @Test
    void returnsFallbackAfterExhaustingAllKeysOnFailure() throws IOException, InterruptedException {
        GeminiProperties properties = new GeminiProperties("key-one,key-two", "gemini-2.0-flash", 3000, 8000);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("connection refused"));
        GeminiAdvisoryClient client = new GeminiAdvisoryClient(httpClient, objectMapper, properties);

        AdvisoryResult result = client.generateAdvisory(CONTEXT);

        assertThat(result.fallbackUsed()).isTrue();
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void rotatesToSecondKeyAfterFirstKeyReturnsRateLimitStatus() throws IOException, InterruptedException {
        GeminiProperties properties = new GeminiProperties("key-one,key-two", "gemini-2.0-flash", 3000, 8000);
        HttpResponse<String> rateLimited = mock(HttpResponse.class);
        when(rateLimited.statusCode()).thenReturn(429);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Tasarruf öneriniz hazır.\"}]}}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(rateLimited)
                .thenReturn(httpResponse);
        GeminiAdvisoryClient client = new GeminiAdvisoryClient(httpClient, objectMapper, properties);

        AdvisoryResult result = client.generateAdvisory(CONTEXT);

        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.content()).isEqualTo("Tasarruf öneriniz hazır.");
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
