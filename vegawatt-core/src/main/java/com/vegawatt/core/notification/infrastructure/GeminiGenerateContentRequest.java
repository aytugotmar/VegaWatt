package com.vegawatt.core.notification.infrastructure;

import java.util.List;

record GeminiGenerateContentRequest(List<Content> contents) {

    static GeminiGenerateContentRequest ofPrompt(String prompt) {
        return new GeminiGenerateContentRequest(List.of(new Content(List.of(new Part(prompt)))));
    }

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }
}
