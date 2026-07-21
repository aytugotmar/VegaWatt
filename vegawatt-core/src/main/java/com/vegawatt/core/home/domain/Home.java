package com.vegawatt.core.home.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Home {

    private final UUID id;
    private final String name;
    private final String contactEmail;
    private final BigDecimal energyQuotaKwh;
    private final BigDecimal budgetQuotaTry;
    private final BigDecimal baseTariffPerKwh;
    private final BigDecimal penaltyTariffPerKwh;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<Appliance> appliances = new ArrayList<>();

    private Home(UUID id, String name, String contactEmail, BigDecimal energyQuotaKwh,
                  BigDecimal budgetQuotaTry, BigDecimal baseTariffPerKwh, BigDecimal penaltyTariffPerKwh,
                  Instant createdAt, Instant updatedAt) {
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

    public static Home register(String name, String contactEmail, BigDecimal energyQuotaKwh,
                                 BigDecimal budgetQuotaTry, BigDecimal baseTariffPerKwh,
                                 BigDecimal penaltyTariffPerKwh, Instant createdAt) {
        if (energyQuotaKwh.signum() <= 0) {
            throw new InvalidHomeConfigurationException("energyQuotaKwh must be positive");
        }
        if (budgetQuotaTry.signum() <= 0) {
            throw new InvalidHomeConfigurationException("budgetQuotaTry must be positive");
        }
        if (baseTariffPerKwh.signum() <= 0) {
            throw new InvalidHomeConfigurationException("baseTariffPerKwh must be positive");
        }
        if (penaltyTariffPerKwh.compareTo(baseTariffPerKwh) < 0) {
            throw new InvalidHomeConfigurationException("penaltyTariffPerKwh must be >= baseTariffPerKwh");
        }
        return new Home(UUID.randomUUID(), name, contactEmail, energyQuotaKwh, budgetQuotaTry,
                baseTariffPerKwh, penaltyTariffPerKwh, createdAt, createdAt);
    }

    public static Home reconstitute(UUID id, String name, String contactEmail, BigDecimal energyQuotaKwh,
                                     BigDecimal budgetQuotaTry, BigDecimal baseTariffPerKwh,
                                     BigDecimal penaltyTariffPerKwh, Instant createdAt, Instant updatedAt,
                                     List<Appliance> appliances) {
        Home home = new Home(id, name, contactEmail, energyQuotaKwh, budgetQuotaTry, baseTariffPerKwh,
                penaltyTariffPerKwh, createdAt, updatedAt);
        home.appliances.addAll(appliances);
        return home;
    }

    public void addAppliance(Appliance appliance) {
        boolean nameTaken = appliances.stream()
                .anyMatch(existing -> existing.name().equalsIgnoreCase(appliance.name()));
        if (nameTaken) {
            throw new DuplicateApplianceNameException(appliance.name());
        }
        appliances.add(appliance);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String contactEmail() {
        return contactEmail;
    }

    public BigDecimal energyQuotaKwh() {
        return energyQuotaKwh;
    }

    public BigDecimal budgetQuotaTry() {
        return budgetQuotaTry;
    }

    public BigDecimal baseTariffPerKwh() {
        return baseTariffPerKwh;
    }

    public BigDecimal penaltyTariffPerKwh() {
        return penaltyTariffPerKwh;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<Appliance> appliances() {
        return Collections.unmodifiableList(appliances);
    }
}
