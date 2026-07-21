package com.vegawatt.core.home.domain;

import java.util.Optional;
import java.util.UUID;

public interface ApplianceRepository {

    Optional<Appliance> findById(UUID applianceId);
}
