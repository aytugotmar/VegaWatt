package com.vegawatt.core.notification.domain;

public interface EmailSenderPort {

    void send(String toAddress, String subject, String body);
}
