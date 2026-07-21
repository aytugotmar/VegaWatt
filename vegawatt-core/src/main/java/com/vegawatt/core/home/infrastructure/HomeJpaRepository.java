package com.vegawatt.core.home.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface HomeJpaRepository extends JpaRepository<HomeEntity, UUID> {
}
