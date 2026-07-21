package com.vegawatt.core.notification.infrastructure;

import com.vegawatt.core.common.config.VegaWattMailProperties;
import com.vegawatt.core.notification.domain.EmailDeliveryException;
import com.vegawatt.core.notification.domain.EmailSenderPort;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class SmtpEmailSender implements EmailSenderPort {

    private final JavaMailSender javaMailSender;
    private final VegaWattMailProperties mailProperties;

    SmtpEmailSender(JavaMailSender javaMailSender, VegaWattMailProperties mailProperties) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(String toAddress, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(toAddress);
        message.setSubject(subject);
        message.setText(body);
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            throw new EmailDeliveryException("Failed to send advisory email to " + toAddress, e);
        }
    }
}
