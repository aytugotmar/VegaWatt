package com.vegawatt.sensors.registration;

/** A well-formed registration event whose eventVersion this consumer doesn't know how to
 * interpret — never worth retrying, since the version won't change on redelivery. */
public class UnsupportedRegistrationEventVersionException extends RuntimeException {

    public UnsupportedRegistrationEventVersionException(String message) {
        super(message);
    }
}
