package com.vegawatt.core.home.domain;

import java.util.Optional;
import java.util.UUID;

public interface HomeRepository {

    Home save(Home home);

    Optional<Home> findById(UUID id);
}
