package com.vegawatt.core.home.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplianceRepository {

    Optional<Appliance> findById(UUID applianceId);

    List<Appliance> findAllByHomeId(UUID homeId);
}
