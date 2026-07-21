package com.vegawatt.core.home.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "homes")
class HomeEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "energy_quota_kwh", nullable = false)
    private BigDecimal energyQuotaKwh;

    @Column(name = "budget_quota_try", nullable = false)
    private BigDecimal budgetQuotaTry;

    @Column(name = "base_tariff_per_kwh", nullable = false)
    private BigDecimal baseTariffPerKwh;

    @Column(name = "penalty_tariff_per_kwh", nullable = false)
    private BigDecimal penaltyTariffPerKwh;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HomeEntity() {
    }

    HomeEntity(UUID id, String name, String contactEmail, BigDecimal energyQuotaKwh, BigDecimal budgetQuotaTry,
               BigDecimal baseTariffPerKwh, BigDecimal penaltyTariffPerKwh, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.energyQuotaKwh = energyQuotaKwh;
        this.budgetQuotaTry = budgetQuotaTry;
        this.baseTariffPerKwh = baseTariffPerKwh;
        this.penaltyTariffPerKwh = penaltyTariffPerKwh;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getContactEmail() {
        return contactEmail;
    }

    BigDecimal getEnergyQuotaKwh() {
        return energyQuotaKwh;
    }

    BigDecimal getBudgetQuotaTry() {
        return budgetQuotaTry;
    }

    BigDecimal getBaseTariffPerKwh() {
        return baseTariffPerKwh;
    }

    BigDecimal getPenaltyTariffPerKwh() {
        return penaltyTariffPerKwh;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
