package com.vegawatt.core.common;

public abstract class ResourceConflictException extends RuntimeException {

    protected ResourceConflictException(String message) {
        super(message);
    }
}
