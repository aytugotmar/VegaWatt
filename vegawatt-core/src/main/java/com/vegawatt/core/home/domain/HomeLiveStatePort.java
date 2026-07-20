package com.vegawatt.core.home.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public interface HomeLiveStatePort {

    void initialize(HomeLiveState state);

    Optional<HomeLiveState> get(UUID homeId);

    List<HomeLiveState> getAll();

    HomeLiveState update(UUID homeId, UnaryOperator<HomeLiveState> mutator);
}
