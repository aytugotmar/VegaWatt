package com.vegawatt.core.home.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeRepository {

    Home save(Home home);

    Optional<Home> findById(UUID id);

    List<Home> findAll();
}
