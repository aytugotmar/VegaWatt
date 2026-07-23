package com.vegawatt.core.insight.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskInsightRequest(
        @NotBlank(message = "Question must not be blank")
        @Size(max = 500, message = "Question must not exceed 500 characters")
        String question
) {}
