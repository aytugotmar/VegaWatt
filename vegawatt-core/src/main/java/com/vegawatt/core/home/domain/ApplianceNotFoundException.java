package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.ResourceNotFoundException;
import java.util.UUID;

public class ApplianceNotFoundException extends ResourceNotFoundException {

    public ApplianceNotFoundException(UUID applianceId) {
        super("Appliance not found: " + applianceId);
    }
}
