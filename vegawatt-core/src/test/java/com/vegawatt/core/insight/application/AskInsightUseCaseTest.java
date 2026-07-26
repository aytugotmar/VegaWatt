package com.vegawatt.core.insight.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.access.domain.HomeAuthorizationService;
import com.vegawatt.core.common.config.GeminiProperties;
import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.insight.api.AskInsightRequest;
import com.vegawatt.core.insight.api.AskInsightResponse;
import com.vegawatt.core.user.domain.UserRole;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AskInsightUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-07-25T10:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();

    @Mock
    private HomeRepository homeRepository;
    @Mock
    private HomeLiveStatePort homeLiveStatePort;
    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;
    @Mock
    private HomeAuthorizationService homeAuthorizationService;

    private AskInsightUseCase useCase() {
        // No Gemini API key configured -> execute() always falls back to the deterministic
        // template answer without making any HTTP call, which is all this test needs.
        GeminiProperties properties = new GeminiProperties("", "gemini-2.0-flash", 2000, 5000, 15000);
        return new AskInsightUseCase(homeRepository, homeLiveStatePort, applianceLiveStatePort,
                homeAuthorizationService, HttpClient.newHttpClient(), new ObjectMapper(), properties);
    }

    private static Home home() {
        return Home.register("Ev", "owner@vegawatt.com", new BigDecimal("500"), new BigDecimal("1000"),
                new BigDecimal("2"), new BigDecimal("3"), NOW);
    }

    @Test
    void queriesOnlyTheAskedHomesAppliancesNotEverySystemAppliance() {
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), UserRole.USER);
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home()));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of());

        AskInsightResponse response = useCase().execute(currentUser, HOME_ID, new AskInsightRequest("Ne kadar harcadım?"));

        verify(applianceLiveStatePort).getByHomeId(HOME_ID);
        verify(applianceLiveStatePort, never()).getAll();
        assertThat(response.answer()).isNotBlank();
        assertThat(response.fallbackUsed()).isTrue();
    }

    @Test
    void mentionsTheHighestPowerApplianceOfTheAskedHomeOnly() {
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), UserRole.USER);
        UUID applianceId = UUID.randomUUID();
        ApplianceLiveState ownHomeAppliance = new ApplianceLiveState(HOME_ID, applianceId, "Buzdolabı",
                "REFRIGERATOR", new BigDecimal("250"), new BigDecimal("180"), null, null,
                BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false, ApplianceHealthStatus.NORMAL, NOW, null, 0L,
                0L);
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home()));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of(ownHomeAppliance));

        AskInsightResponse response = useCase().execute(currentUser, HOME_ID, new AskInsightRequest("En çok hangi cihaz tüketiyor?"));

        assertThat(response.answer()).contains("Buzdolabı");
    }

    @Test
    void skipsRemainingKeyModelCombinationsOnceTheOverallTimeoutIsAlreadyExhausted() throws Exception {
        // A negative overallTimeoutMs puts the deadline in the past before the loop even starts,
        // simulating "we already spent our whole budget retrying" without needing a slow/mocked
        // HTTP round trip to actually elapse real time in the test.
        GeminiProperties properties = new GeminiProperties("dummy-key", "gemini-2.0-flash", 2000, 5000, -1000);
        HttpClient mockHttpClient = mock(HttpClient.class);
        AskInsightUseCase useCase = new AskInsightUseCase(homeRepository, homeLiveStatePort, applianceLiveStatePort,
                homeAuthorizationService, mockHttpClient, new ObjectMapper(), properties);
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), UserRole.USER);
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home()));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of());

        AskInsightResponse response =
                useCase.execute(currentUser, HOME_ID, new AskInsightRequest("Ne kadar harcadım?"));

        assertThat(response.fallbackUsed()).isTrue();
        verify(mockHttpClient, never()).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void treatsAnHttp200WithABlankAnswerAsFailureNotSuccess() throws Exception {
        GeminiProperties properties = new GeminiProperties("dummy-key", "gemini-2.0-flash", 2000, 5000, 15000);
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> blankAnswer = mock(HttpResponse.class);
        when(blankAnswer.statusCode()).thenReturn(200);
        when(blankAnswer.body()).thenReturn(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"   \"}]}}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(blankAnswer);
        AskInsightUseCase useCase = new AskInsightUseCase(homeRepository, homeLiveStatePort, applianceLiveStatePort,
                homeAuthorizationService, mockHttpClient, new ObjectMapper(), properties);
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), UserRole.USER);
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home()));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of());

        AskInsightResponse response =
                useCase.execute(currentUser, HOME_ID, new AskInsightRequest("Ne kadar harcadım?"));

        assertThat(response.fallbackUsed())
                .as("a blank answer must not be reported as a successful Gemini response")
                .isTrue();
        // properties.model() == "gemini-2.0-flash" is also one of the two hardcoded fallback
        // models, so distinct() collapses modelsToTry to 2 entries, not 3 — exactly 2 attempts.
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deduplicatesTheConfiguredModelFromTheHardcodedFallbackList() throws Exception {
        GeminiProperties properties = new GeminiProperties("dummy-key", "gemini-2.0-flash", 2000, 5000, 15000);
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> serverError = mock(HttpResponse.class);
        when(serverError.statusCode()).thenReturn(500);
        when(serverError.body()).thenReturn("{}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(serverError);
        AskInsightUseCase useCase = new AskInsightUseCase(homeRepository, homeLiveStatePort, applianceLiveStatePort,
                homeAuthorizationService, mockHttpClient, new ObjectMapper(), properties);
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), UserRole.USER);
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home()));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of());

        useCase.execute(currentUser, HOME_ID, new AskInsightRequest("Ne kadar harcadım?"));

        ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(2)).send(sent.capture(), any(HttpResponse.BodyHandler.class));
        Set<String> distinctUris = sent.getAllValues().stream()
                .map(request -> request.uri().toString())
                .collect(Collectors.toSet());
        assertThat(distinctUris).as("each model should only be attempted once, not re-tried under its own name")
                .hasSize(2);
    }
}
