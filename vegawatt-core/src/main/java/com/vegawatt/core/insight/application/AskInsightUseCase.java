package com.vegawatt.core.insight.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.access.domain.HomeAuthorizationService;
import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.common.config.GeminiProperties;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.insight.api.AskInsightRequest;
import com.vegawatt.core.insight.api.AskInsightResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AskInsightUseCase {

    private static final Logger log = LoggerFactory.getLogger(AskInsightUseCase.class);

    private static final String FALLBACK_ANSWER =
            "Şu an detaylı AI analizi yapılamıyor. Lütfen evinizin güncel tüketim grafiklerini ve cihaz listesini panel üzerinden inceleyin.";

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final HomeRepository homeRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final HomeAuthorizationService homeAuthorizationService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;
    private final List<String> apiKeys;

    public AskInsightUseCase(HomeRepository homeRepository, HomeLiveStatePort homeLiveStatePort,
                             ApplianceLiveStatePort applianceLiveStatePort,
                             HomeAuthorizationService homeAuthorizationService,
                             HttpClient httpClient, ObjectMapper objectMapper,
                             GeminiProperties properties) {
        this.homeRepository = homeRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.homeAuthorizationService = homeAuthorizationService;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.apiKeys = Arrays.stream(properties.apiKey().split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList();
    }

    public AskInsightResponse execute(CurrentUser currentUser, UUID homeId, AskInsightRequest request) {
        homeAuthorizationService.requireAccess(currentUser, homeId);

        Home home = homeRepository.findById(homeId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        HomeLiveState liveState = homeLiveStatePort.get(homeId).orElse(null);
        List<ApplianceLiveState> applianceStates = applianceLiveStatePort.getAll().stream()
                .filter(a -> a.homeId().equals(homeId))
                .toList();

        BigDecimal currentCost = liveState != null ? liveState.currentCost().amount() : BigDecimal.ZERO;
        BigDecimal monthlyBudget = home.budgetQuotaTry() != null ? home.budgetQuotaTry() : new BigDecimal("1000");

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
        int daysInMonth = YearMonth.from(today).lengthOfMonth();
        int elapsedDays = Math.max(1, today.getDayOfMonth());
        int remainingDays = daysInMonth - elapsedDays;

        BigDecimal avgDailyCost = currentCost.divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP);
        BigDecimal projectedMonthEndCost = currentCost.add(avgDailyCost.multiply(BigDecimal.valueOf(remainingDays)));

        String fallbackAnswer = generateSmartFallbackAnswer(home, currentCost, monthlyBudget, projectedMonthEndCost, applianceStates, request.question());

        if (apiKeys.isEmpty()) {
            return new AskInsightResponse(fallbackAnswer, true);
        }

        String prompt = buildPrompt(home, currentCost, monthlyBudget, elapsedDays, remainingDays, projectedMonthEndCost, applianceStates, request.question());

        List<String> modelsToTry = List.of(properties.model(), "gemini-1.5-flash", "gemini-2.0-flash");
        for (int i = 0; i < apiKeys.size(); i++) {
            for (String modelName : modelsToTry) {
                try {
                    String answer = callGemini(apiKeys.get(i), modelName, prompt);
                    return new AskInsightResponse(answer, false);
                } catch (Exception e) {
                    log.warn("Gemini call failed (key #{}, model {}): {}", i + 1, modelName, e.getMessage());
                }
            }
        }

        return new AskInsightResponse(fallbackAnswer, true);
    }

    private String generateSmartFallbackAnswer(Home home, BigDecimal currentCost, BigDecimal budget,
                                                BigDecimal projectedCost, List<ApplianceLiveState> appliances, String question) {
        String lowerQ = question != null ? question.toLowerCase() : "";
        ApplianceLiveState maxApp = appliances.stream()
                .max((a, b) -> a.currentPowerWatt().compareTo(b.currentPowerWatt()))
                .orElse(null);

        if (lowerQ.contains("bütçe") || lowerQ.contains("aşar") || lowerQ.contains("fatura")) {
            boolean exceeds = projectedCost.compareTo(budget) > 0;
            if (exceeds) {
                BigDecimal diff = projectedCost.subtract(budget);
                return String.format(
                        "%s evinizde bu ay şu ana kadar ₺%.2f harcama yapıldı. Mevcut tüketim temposuyla ay sonu faturanızın ₺%.2f olması ve ₺%.2f hedef bütçenizi yaklaşık ₺%.2f aşması öngörülüyor. %s",
                        home.name(), currentCost, projectedCost, budget, diff,
                        maxApp != null ? String.format("Aşımı önlemek için şu an %sW çeken '%s' gibi yüksek tüketimli cihazları daha kısa süreli çalıştırmanızı öneririz.", maxApp.currentPowerWatt(), maxApp.applianceName()) : "Enerji tasarrufu için cihazlarınızı ekonomik modda kullanabilirsiniz."
                );
            } else {
                return String.format(
                        "%s evinizde şu ana kadar ₺%.2f harcama yaptınız. Tahmini ay sonu faturanız ₺%.2f olup ₺%.2f tutarındaki hedef bütçenizin altında seyretmektedir. Harika bir enerji yönetimi!",
                        home.name(), currentCost, projectedCost, budget
                );
            }
        }

        if (lowerQ.contains("cihaz") || lowerQ.contains("tüket") || lowerQ.contains("harcıyor") || lowerQ.contains("yüksek")) {
            if (maxApp != null) {
                return String.format(
                        "%s evinizde anlık olarak en çok enerji tüketen cihaz %sW çekimle '%s' cihazıdır. %s",
                        home.name(), maxApp.currentPowerWatt(), maxApp.applianceName(),
                        maxApp.anomalous() ? "Dikkat: Bu cihazda anomali/yüksek güç uyarısı tespit edilmiştir!" : "Cihazın çalışma durumunu panel üzerinden takip edebilirsiniz."
                );
            }
        }

        if (lowerQ.contains("ne kadar") || lowerQ.contains("kaç") || lowerQ.contains("tutar")) {
            return String.format(
                    "%s evinizin bu ayki güncel harcaması ₺%.2f seviyesindedir. Ay sonu tahmini faturanız ise ₺%.2f olarak hesaplanmıştır (Hedef Bütçe: ₺%.2f).",
                    home.name(), currentCost, projectedCost, budget
            );
        }

        return String.format(
                "%s evinizin bu ayki güncel harcaması ₺%.2f, tahmini ay sonu faturanız ise ₺%.2f seviyesindedir (Aylık Bütçe: ₺%.2f). %s",
                home.name(), currentCost, projectedCost, budget,
                maxApp != null ? String.format("En çok güç çeken cihazınız %sW ile %s cihazıdır.", maxApp.currentPowerWatt(), maxApp.applianceName()) : "Cihazlarınızı kontrollü kullanarak bütçenizi koruyabilirsiniz."
        );
    }

    private String buildPrompt(Home home, BigDecimal currentCost, BigDecimal budget, int elapsedDays,
                               int remainingDays, BigDecimal projectedCost, List<ApplianceLiveState> appliances, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sen akıllı bir ev enerji danışmanısın. Kullanıcının enerji kullanımına ilişkin sorusunu ")
                .append("Türkçe, samimi, anlaşılır, düz metin olarak (başlık veya markdown simgeleri kullanmadan) yanıtla.\n\n")
                .append("Ev Adı: ").append(home.name()).append("\n")
                .append("Şu ana kadar ki harcama: ").append(currentCost).append(" TRY\n")
                .append("Aylık Hedef Bütçe: ").append(budget).append(" TRY\n")
                .append("Ayın ").append(elapsedDays).append(". günü (kalan gün: ").append(remainingDays).append(")\n")
                .append("Tahmini Ay Sonu Faturası: ").append(projectedCost).append(" TRY\n");

        if (!appliances.isEmpty()) {
            sb.append("Evdeki Cihazlar ve Güçleri: ");
            for (ApplianceLiveState app : appliances) {
                sb.append(app.applianceName()).append(" (").append(app.currentPowerWatt()).append("W, ")
                  .append(app.operatingState()).append("), ");
            }
            sb.append("\n");
        }

        sb.append("Kullanıcı Sorusu: ").append(question).append("\n");
        return sb.toString();
    }

    private String callGemini(String apiKey, String modelName, String prompt) throws Exception {
        String reqBody = objectMapper.writeValueAsString(
                java.util.Map.of("contents", List.of(java.util.Map.of("parts", List.of(java.util.Map.of("text", prompt))))));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT_TEMPLATE.formatted(modelName)))
                .timeout(Duration.ofMillis(properties.readTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Gemini API error (status {}): {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText().trim();
    }
}
