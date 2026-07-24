package com.vegawatt.core.user.domain;

import com.vegawatt.core.common.ResourceNotFoundException;
import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
