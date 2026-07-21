package com.vegawatt.core.common;

public abstract class UnauthorizedException extends RuntimeException {

    protected UnauthorizedException(String message) {
        super(message);
    }
}
