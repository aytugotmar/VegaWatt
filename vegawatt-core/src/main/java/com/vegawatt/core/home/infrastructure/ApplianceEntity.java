package com.vegawatt.core.home.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appliances")
class ApplianceEntity {

    @Id
    private UUID id;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "safe_power_limit_watt", nullable = false)
    private BigDecimal safePowerLimitWatt;

    @Column(name = "simulation_min_watt", nullable = false)
    private BigDecimal simulationMinWatt;

    @Column(name = "simulation_max_watt", nullable = false)
    private BigDecimal simulationMaxWatt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApplianceEntity() {
    }

    ApplianceEntity(UUID id, UUID homeId, String name, String type, BigDecimal safePowerLimitWatt,
                     BigDecimal simulationMinWatt, BigDecimal simulationMaxWatt, boolean active, Instant createdAt) {
        this.id = id;
        this.homeId = homeId;
        this.name = name;
        this.type = type;
        this.safePowerLimitWatt = safePowerLimitWatt;
        this.simulationMinWatt = simulationMinWatt;
        this.simulationMaxWatt = simulationMaxWatt;
        this.active = active;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getHomeId() {
        return homeId;
    }

    String getName() {
        return name;
    }

    String getType() {
        return type;
    }

    BigDecimal getSafePowerLimitWatt() {
        return safePowerLimitWatt;
    }

    BigDecimal getSimulationMinWatt() {
        return simulationMinWatt;
    }

    BigDecimal getSimulationMaxWatt() {
        return simulationMaxWatt;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
