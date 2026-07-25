package com.vegawatt.sensors.registration;

/** A registration event whose payload could not be parsed at all — never worth retrying. */
public class MalformedRegistrationEventException extends RuntimeException {

    public MalformedRegistrationEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
