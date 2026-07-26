package com.vegawatt.core.history.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consumption_snapshots")
class ConsumptionSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime;

    @Column(name = "accumulated_energy_kwh", nullable = false)
    private BigDecimal accumulatedEnergyKwh;

    @Column(name = "accumulated_cost", nullable = false)
    private BigDecimal accumulatedCost;

    @Column(name = "tariff_state", nullable = false)
    private String tariffState;

    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;

    protected ConsumptionSnapshotEntity() {
    }

    ConsumptionSnapshotEntity(UUID id, UUID homeId, Instant snapshotTime, BigDecimal accumulatedEnergyKwh,
                               BigDecimal accumulatedCost, String tariffState, String billingPeriod) {
        this.id = id;
        this.homeId = homeId;
        this.snapshotTime = snapshotTime;
        this.accumulatedEnergyKwh = accumulatedEnergyKwh;
        this.accumulatedCost = accumulatedCost;
        this.tariffState = tariffState;
        this.billingPeriod = billingPeriod;
    }

    UUID getId() {
        return id;
    }

    UUID getHomeId() {
        return homeId;
    }

    Instant getSnapshotTime() {
        return snapshotTime;
    }

    BigDecimal getAccumulatedEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    BigDecimal getAccumulatedCost() {
        return accumulatedCost;
    }

    String getTariffState() {
        return tariffState;
    }

    String getBillingPeriod() {
        return billingPeriod;
    }
}
