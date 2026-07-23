package com.vegawatt.core.home.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterHomeRequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsApplianceWithoutCatalogItemIdAndWithoutPowerRange() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Klima", "AC", null, null, null, null);

        Set<ConstraintViolation<RegisterHomeRequest.ApplianceRequest>> violations = VALIDATOR.validate(appliance);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void acceptsApplianceWithoutCatalogItemIdWhenPowerRangeProvided() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Klima", "AC", new BigDecimal("2500"),
                new BigDecimal("200"), new BigDecimal("2300"), null);

        Set<ConstraintViolation<RegisterHomeRequest.ApplianceRequest>> violations = VALIDATOR.validate(appliance);

        assertThat(violations).isEmpty();
    }

    @Test
    void acceptsApplianceWithCatalogItemIdAndWithoutPowerRange() {
        var appliance = new RegisterHomeRequest.ApplianceRequest("Kahve Makinesi", "COFFEE_MACHINE", null, null,
                null, UUID.randomUUID());

        Set<ConstraintViolation<RegisterHomeRequest.ApplianceRequest>> violations = VALIDATOR.validate(appliance);

        assertThat(violations).isEmpty();
    }
}
