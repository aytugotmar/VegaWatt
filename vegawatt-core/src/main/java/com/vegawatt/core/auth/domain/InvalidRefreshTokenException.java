package com.vegawatt.core.auth.domain;

import com.vegawatt.core.common.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
