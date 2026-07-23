package com.vegawatt.core.insight.api;

public record AskInsightResponse(
        String answer,
        boolean fallbackUsed
) {}
