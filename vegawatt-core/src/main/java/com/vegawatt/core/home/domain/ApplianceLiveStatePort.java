package com.vegawatt.core.home.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public interface ApplianceLiveStatePort {

    void initialize(ApplianceLiveState state);

    Optional<ApplianceLiveState> get(UUID homeId, UUID applianceId);

    List<ApplianceLiveState> getByHomeId(UUID homeId);

    List<ApplianceLiveState> getAll();

    ApplianceLiveState update(UUID homeId, UUID applianceId, UnaryOperator<ApplianceLiveState> mutator);
}
