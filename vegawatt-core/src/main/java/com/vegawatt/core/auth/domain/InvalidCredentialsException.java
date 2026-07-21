package com.vegawatt.core.auth.domain;

import com.vegawatt.core.common.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
