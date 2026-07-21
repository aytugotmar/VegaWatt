package com.vegawatt.core.billing.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_accounts")
class BillingAccountEntity {

    @Id
    private UUID id;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;

    @Column(name = "accumulated_energy_kwh", nullable = false)
    private BigDecimal accumulatedEnergyKwh;

    @Column(name = "accumulated_cost", nullable = false)
    private BigDecimal accumulatedCost;

    @Column(name = "penalty_active", nullable = false)
    private boolean penaltyActive;

    @Column(name = "energy_quota_80_notified", nullable = false)
    private boolean energyQuota80Notified;

    @Column(name = "energy_quota_100_notified", nullable = false)
    private boolean energyQuota100Notified;

    @Column(name = "budget_quota_80_notified", nullable = false)
    private boolean budgetQuota80Notified;

    @Column(name = "budget_quota_100_notified", nullable = false)
    private boolean budgetQuota100Notified;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BillingAccountEntity() {
    }

    BillingAccountEntity(UUID id, UUID homeId, String billingPeriod, BigDecimal accumulatedEnergyKwh,
                          BigDecimal accumulatedCost, boolean penaltyActive, boolean energyQuota80Notified,
                          boolean energyQuota100Notified, boolean budgetQuota80Notified,
                          boolean budgetQuota100Notified, long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.homeId = homeId;
        this.billingPeriod = billingPeriod;
        this.accumulatedEnergyKwh = accumulatedEnergyKwh;
        this.accumulatedCost = accumulatedCost;
        this.penaltyActive = penaltyActive;
        this.energyQuota80Notified = energyQuota80Notified;
        this.energyQuota100Notified = energyQuota100Notified;
        this.budgetQuota80Notified = budgetQuota80Notified;
        this.budgetQuota100Notified = budgetQuota100Notified;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getHomeId() {
        return homeId;
    }

    String getBillingPeriod() {
        return billingPeriod;
    }

    BigDecimal getAccumulatedEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    BigDecimal getAccumulatedCost() {
        return accumulatedCost;
    }

    boolean isPenaltyActive() {
        return penaltyActive;
    }

    boolean isEnergyQuota80Notified() {
        return energyQuota80Notified;
    }

    boolean isEnergyQuota100Notified() {
        return energyQuota100Notified;
    }

    boolean isBudgetQuota80Notified() {
        return budgetQuota80Notified;
    }

    boolean isBudgetQuota100Notified() {
        return budgetQuota100Notified;
    }

    long getVersion() {
        return version;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
