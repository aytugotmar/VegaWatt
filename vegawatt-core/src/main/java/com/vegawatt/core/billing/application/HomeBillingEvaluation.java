package com.vegawatt.core.billing.application;

import com.vegawatt.core.home.domain.HomeLiveState;

public record HomeBillingEvaluation(HomeLiveState newState, HomeUpdateOutcome outcome) {
}
