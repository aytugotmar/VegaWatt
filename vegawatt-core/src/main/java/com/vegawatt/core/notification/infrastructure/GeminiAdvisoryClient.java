package com.vegawatt.core.notification.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.config.GeminiProperties;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AdvisoryResult;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.EnergyAdvisoryPort;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class GeminiAdvisoryClient implements EnergyAdvisoryPort {

    private static final Logger log = LoggerFactory.getLogger(GeminiAdvisoryClient.class);

    private static final String FALLBACK_MESSAGE =
            "Enerji kullanımınız belirlenen sınıra yaklaştı veya ulaştı. Yüksek tüketimli cihazları "
                    + "kontrol ederek kullanımınızı azaltmanız önerilir.";

    // I send the key as a header rather than as a ?key= query parameter. Query strings are
    // recorded by access logs and by any proxy in between, and in this codebase there is a
    // concrete path to disk: NotificationOrchestrator catches RuntimeException and logs the
    // exception with its stack trace, so anything thrown out of URI.create would print the
    // full URL, key included. With the key in a header that cannot happen regardless of
    // which handler catches what.
    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;
    private final List<String> apiKeys;

    GeminiAdvisoryClient(HttpClient httpClient, ObjectMapper objectMapper, GeminiProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.apiKeys = Arrays.stream(properties.apiKey().split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .toList();
    }

    @Override
    public AdvisoryResult generateAdvisory(AdvisoryContext context) {
        if (apiKeys.isEmpty()) {
            log.warn("No Gemini API key configured, returning fallback advisory for home {}", context.homeId());
            return new AdvisoryResult(FALLBACK_MESSAGE, true);
        }

        String prompt = buildPrompt(context);
        for (int keyIndex = 0; keyIndex < apiKeys.size(); keyIndex++) {
            try {
                String text = callGemini(apiKeys.get(keyIndex), prompt);
                return new AdvisoryResult(text, false);
            } catch (GeminiCallException e) {
                log.warn("Gemini call failed (key #{}, status={}), trying next key", keyIndex + 1,
                        e.statusCode());
            } catch (IOException e) {
                log.warn("Gemini call failed (key #{}): {}", keyIndex + 1, e.getClass().getSimpleName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Gemini call interrupted (key #{})", keyIndex + 1);
            }
        }

        log.warn("All Gemini API keys exhausted for home {}, returning fallback advisory", context.homeId());
        return new AdvisoryResult(FALLBACK_MESSAGE, true);
    }

    private String callGemini(String apiKey, String prompt) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(GeminiGenerateContentRequest.ofPrompt(prompt));
        String url = ENDPOINT_TEMPLATE.formatted(properties.model());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.readTimeoutMs()))
                .header("Content-Type", "application/json")
                .header(API_KEY_HEADER, apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Gemini Advisory API error (status {}): {}", response.statusCode(), response.body());
            throw new GeminiCallException(response.statusCode());
        }
        return extractText(response.body());
    }

    private String extractText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!textNode.isTextual()) {
            throw new IOException("Unexpected Gemini response shape");
        }
        return textNode.asText().trim();
    }

    private static String buildPrompt(AdvisoryContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Sen bir ev enerji tasarrufu danışmanısın. Aşağıdaki bilgilere dayanarak ev sahibine ")
                .append("Türkçe, düz metin olarak (markdown, yıldız veya başlık işareti kullanmadan), ")
                .append("kısa ve uygulanabilir bir enerji tasarrufu önerisi yaz.\n\n")
                .append("Ev adı: ").append(context.homeName()).append('\n')
                .append("Bildirim nedeni: ").append(describeTrigger(context.triggerType())).append('\n')
                .append("Enerji kotası kullanım oranı: %").append(context.energyQuotaPercentage()).append('\n')
                .append("Bütçe kotası kullanım oranı: %").append(context.budgetQuotaPercentage()).append('\n')
                .append("Güncel fatura tutarı: ").append(context.currentCost().rounded()).append(" TRY\n")
                .append("Aktif tarife: ")
                .append(context.tariffState() == TariffState.PENALTY ? "ceza tarifesi" : "normal tarife")
                .append('\n');

        if (!context.anomalousApplianceNames().isEmpty()) {
            prompt.append("Anormal tüketim gösteren cihazlar: ")
                    .append(String.join(", ", context.anomalousApplianceNames()))
                    .append('\n');
        }

        return prompt.toString();
    }

    private static String describeTrigger(AdvisoryTriggerType triggerType) {
        return switch (triggerType) {
            case QUOTA_80 -> "enerji veya bütçe kotasının yüzde 80'ine ulaşıldı";
            case QUOTA_100 -> "enerji veya bütçe kotası aşıldı, ceza tarifesi devrede olabilir";
            case ANOMALY -> "bir cihazda üç ardışık ölçümde güvenli güç sınırı aşıldı";
            case STANDBY_ANOMALY -> "bir cihaz bekleme modundayken beklenenden çok daha yüksek güç tüketiyor";
            case TELEMETRY_STALE -> "bir cihazdan beklenenden uzun süredir veri gelmiyor, gecikme olabilir";
            case TELEMETRY_OFFLINE -> "bir cihaz uzun süredir hiç veri göndermiyor, muhtemelen kapalı veya "
                    + "bağlantısı kesilmiş";
        };
    }
}
