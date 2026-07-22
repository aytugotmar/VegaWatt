package com.vegawatt.core.notification.application;

import com.vegawatt.core.notification.domain.AdvisoryEmailDispatchException;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.AiRecommendationRepository;
import com.vegawatt.core.notification.domain.EmailDeliveryException;
import com.vegawatt.core.notification.domain.EmailSenderPort;
import com.vegawatt.core.notification.domain.EmailStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DispatchAdvisoryEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(DispatchAdvisoryEmailUseCase.class);
    private static final String SUBJECT = "VegaWatt Enerji Tasarrufu Önerisi";

    private final EmailSenderPort emailSenderPort;
    private final AiRecommendationRepository aiRecommendationRepository;

    public DispatchAdvisoryEmailUseCase(EmailSenderPort emailSenderPort,
                                         AiRecommendationRepository aiRecommendationRepository) {
        this.emailSenderPort = emailSenderPort;
        this.aiRecommendationRepository = aiRecommendationRepository;
    }

    /**
     * Sends the advisory and records the outcome on the recommendation.
     *
     * <p>I record FAILED and then rethrow, rather than swallowing the failure. Swallowing it let
     * the caller mark the notification job SENT while the recommendation said FAILED: two
     * contradictory records of one operation, and the mail was never retried even though the
     * retry machinery was sitting right there. Rethrowing puts a failed send back on the same
     * path as every other failure.
     */
    public void execute(AiRecommendation recommendation, String contactEmail) {
        try {
            emailSenderPort.send(contactEmail, SUBJECT, recommendation.content());
            aiRecommendationRepository.save(recommendation.withEmailStatus(EmailStatus.SENT));
        } catch (EmailDeliveryException e) {
            log.error("Failed to send advisory email for home {}", recommendation.homeId(), e);
            aiRecommendationRepository.save(recommendation.withEmailStatus(EmailStatus.FAILED));
            throw new AdvisoryEmailDispatchException(
                    "Advisory email delivery failed for home " + recommendation.homeId(), e);
        }
    }
}
