package com.vegawatt.core.telemetry.infrastructure;

/** A well-formed telemetry event whose eventVersion this consumer doesn't know how to interpret —
 * never worth retrying, since the version won't change on redelivery. Mirrors
 * {@code UnsupportedRegistrationEventVersionException} in vegawatt-telemetry-sensors' registration
 * consumer, which this module's telemetry consumer never had an equivalent of. */
public class UnsupportedTelemetryEventVersionException extends RuntimeException {

    UnsupportedTelemetryEventVersionException(String message) {
        super(message);
    }
}
