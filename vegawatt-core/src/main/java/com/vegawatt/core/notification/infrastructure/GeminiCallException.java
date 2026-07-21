package com.vegawatt.core.notification.infrastructure;

class GeminiCallException extends RuntimeException {

    private final int statusCode;

    GeminiCallException(int statusCode) {
        super("Gemini API call failed with status " + statusCode);
        this.statusCode = statusCode;
    }

    int statusCode() {
        return statusCode;
    }
}
