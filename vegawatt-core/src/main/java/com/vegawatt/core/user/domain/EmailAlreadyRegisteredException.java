package com.vegawatt.core.user.domain;

import com.vegawatt.core.common.ResourceConflictException;

public class EmailAlreadyRegisteredException extends ResourceConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
