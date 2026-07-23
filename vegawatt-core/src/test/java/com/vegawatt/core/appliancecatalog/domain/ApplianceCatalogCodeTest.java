package com.vegawatt.core.appliancecatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApplianceCatalogCodeTest {

    @Test
    void acceptsValidUppercaseCode() {
        ApplianceCatalogCode code = new ApplianceCatalogCode("COFFEE_MACHINE");

        assertThat(code.value()).isEqualTo("COFFEE_MACHINE");
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new ApplianceCatalogCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLowercaseValue() {
        assertThatThrownBy(() -> new ApplianceCatalogCode("coffee_machine"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooShortValue() {
        assertThatThrownBy(() -> new ApplianceCatalogCode("A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsValueWithInvalidCharacters() {
        assertThatThrownBy(() -> new ApplianceCatalogCode("COFFEE-MACHINE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
