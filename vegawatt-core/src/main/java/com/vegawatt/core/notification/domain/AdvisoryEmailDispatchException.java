package com.vegawatt.core.notification.domain;

/**
 * Thrown when an advisory email could not be delivered.
 *
 * <p>Unchecked on purpose. {@code NotificationOrchestrator} already routes every
 * {@code RuntimeException} into the retry path with backoff and a terminal-failure state, so a
 * failed send takes the same route as any other failure rather than needing its own handling.
 */
public class AdvisoryEmailDispatchException extends RuntimeException {

    public AdvisoryEmailDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
