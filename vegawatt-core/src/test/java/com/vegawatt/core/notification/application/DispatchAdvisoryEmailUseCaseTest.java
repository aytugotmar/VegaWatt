package com.vegawatt.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.vegawatt.core.notification.domain.AdvisoryEmailDispatchException;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.AiRecommendationRepository;
import com.vegawatt.core.notification.domain.EmailDeliveryException;
import com.vegawatt.core.notification.domain.EmailSenderPort;
import com.vegawatt.core.notification.domain.EmailStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DispatchAdvisoryEmailUseCaseTest {

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private AiRecommendationRepository aiRecommendationRepository;

    @Test
    void marksRecommendationFailedAndRethrowsWhenSmtpFails() {
        AiRecommendation recommendation = AiRecommendation.create(UUID.randomUUID(), AdvisoryTriggerType.QUOTA_80,
                "Tasarruf öneriniz", false, Instant.parse("2026-07-20T10:00:00Z"), UUID.randomUUID());
        doThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
                .when(emailSenderPort).send(any(), any(), any());

        DispatchAdvisoryEmailUseCase useCase = new DispatchAdvisoryEmailUseCase(emailSenderPort,
                aiRecommendationRepository);

        // The rethrow is the point. Before this, the failure was swallowed here and the caller
        // went on to mark the notification job SENT, so the recommendation said FAILED and the
        // job said SENT for one operation and no retry ever happened.
        assertThatThrownBy(() -> useCase.execute(recommendation, "home@example.com"))
                .isInstanceOf(AdvisoryEmailDispatchException.class);

        ArgumentCaptor<AiRecommendation> captor = ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository).save(captor.capture());
        assertThat(captor.getValue().emailStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(captor.getValue().content()).isEqualTo("Tasarruf öneriniz");
    }

    @Test
    void marksRecommendationSentWhenEmailSucceeds() {
        AiRecommendation recommendation = AiRecommendation.create(UUID.randomUUID(), AdvisoryTriggerType.ANOMALY,
                "Cihaz uyarısı", false, Instant.parse("2026-07-20T10:00:00Z"), UUID.randomUUID());

        DispatchAdvisoryEmailUseCase useCase = new DispatchAdvisoryEmailUseCase(emailSenderPort,
                aiRecommendationRepository);
        useCase.execute(recommendation, "home@example.com");

        ArgumentCaptor<AiRecommendation> captor = ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository).save(captor.capture());
        assertThat(captor.getValue().emailStatus()).isEqualTo(EmailStatus.SENT);
    }
}
