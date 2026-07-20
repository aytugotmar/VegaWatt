package com.vegawatt.core.notification.domain;

public interface EnergyAdvisoryPort {

    AdvisoryResult generateAdvisory(AdvisoryContext context);
}
