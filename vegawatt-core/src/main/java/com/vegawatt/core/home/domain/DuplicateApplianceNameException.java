package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.ResourceConflictException;

public class DuplicateApplianceNameException extends ResourceConflictException {

    public DuplicateApplianceNameException(String applianceName) {
        super("An appliance named '" + applianceName + "' already exists for this home");
    }
}
