package com.vegawatt.core.notification.application;

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

    public void execute(AiRecommendation recommendation, String contactEmail) {
        try {
            emailSenderPort.send(contactEmail, SUBJECT, recommendation.content());
            aiRecommendationRepository.save(recommendation.withEmailStatus(EmailStatus.SENT));
        } catch (EmailDeliveryException e) {
            log.error("Failed to send advisory email for home {}", recommendation.homeId(), e);
            aiRecommendationRepository.save(recommendation.withEmailStatus(EmailStatus.FAILED));
        }
    }
}
