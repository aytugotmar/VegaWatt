package com.vegawatt.core.billing.domain;

import com.vegawatt.core.common.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BillingAccount {

    private final UUID id;
    private final UUID homeId;
    private final String billingPeriod;
    private BigDecimal accumulatedEnergyKwh;
    private Money accumulatedCost;
    private boolean penaltyActive;
    private boolean energyQuota80Notified;
    private boolean energyQuota100Notified;
    private boolean budgetQuota80Notified;
    private boolean budgetQuota100Notified;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private BillingAccount(UUID id, UUID homeId, String billingPeriod, BigDecimal accumulatedEnergyKwh,
                            Money accumulatedCost, boolean penaltyActive, boolean energyQuota80Notified,
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

    public static BillingAccount open(UUID homeId, String billingPeriod, Instant now) {
        return new BillingAccount(UUID.randomUUID(), homeId, billingPeriod, BigDecimal.ZERO.setScale(9),
                Money.zero(), false, false, false, false, false, 0L, now, now);
    }

    public static BillingAccount reconstitute(UUID id, UUID homeId, String billingPeriod,
                                               BigDecimal accumulatedEnergyKwh, Money accumulatedCost,
                                               boolean penaltyActive, boolean energyQuota80Notified,
                                               boolean energyQuota100Notified, boolean budgetQuota80Notified,
                                               boolean budgetQuota100Notified, long version, Instant createdAt,
                                               Instant updatedAt) {
        return new BillingAccount(id, homeId, billingPeriod, accumulatedEnergyKwh, accumulatedCost, penaltyActive,
                energyQuota80Notified, energyQuota100Notified, budgetQuota80Notified, budgetQuota100Notified,
                version, createdAt, updatedAt);
    }

    public void applyTelemetry(BigDecimal energyIncrementKwh, Money costIncrement, Instant now) {
        this.accumulatedEnergyKwh = this.accumulatedEnergyKwh.add(energyIncrementKwh);
        this.accumulatedCost = this.accumulatedCost.plus(costIncrement);
        this.updatedAt = now;
    }

    public void activatePenalty() {
        this.penaltyActive = true;
    }

    public void markEnergyQuota80Notified() {
        this.energyQuota80Notified = true;
    }

    public void markEnergyQuota100Notified() {
        this.energyQuota100Notified = true;
    }

    public void markBudgetQuota80Notified() {
        this.budgetQuota80Notified = true;
    }

    public void markBudgetQuota100Notified() {
        this.budgetQuota100Notified = true;
    }

    public UUID id() {
        return id;
    }

    public UUID homeId() {
        return homeId;
    }

    public String billingPeriod() {
        return billingPeriod;
    }

    public BigDecimal accumulatedEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    public Money accumulatedCost() {
        return accumulatedCost;
    }

    public boolean penaltyActive() {
        return penaltyActive;
    }

    public boolean energyQuota80Notified() {
        return energyQuota80Notified;
    }

    public boolean energyQuota100Notified() {
        return energyQuota100Notified;
    }

    public boolean budgetQuota80Notified() {
        return budgetQuota80Notified;
    }

    public boolean budgetQuota100Notified() {
        return budgetQuota100Notified;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
