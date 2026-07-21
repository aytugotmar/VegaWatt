package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.ResourceNotFoundException;
import java.util.UUID;

public class HomeNotFoundException extends ResourceNotFoundException {

    public HomeNotFoundException(UUID homeId) {
        super("Home not found: " + homeId);
    }
}
