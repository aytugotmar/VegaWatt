package com.vegawatt.core.appliancecatalog.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appliance_catalog")
class ApplianceCatalogEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "behavior_profile", nullable = false)
    private String behaviorProfile;

    @Column(name = "default_safe_power_limit_watt", nullable = false)
    private BigDecimal defaultSafePowerLimitWatt;

    @Column(name = "default_active_min_watt", nullable = false)
    private BigDecimal defaultActiveMinWatt;

    @Column(name = "default_active_max_watt", nullable = false)
    private BigDecimal defaultActiveMaxWatt;

    @Column(name = "default_standby_min_watt")
    private BigDecimal defaultStandbyMinWatt;

    @Column(name = "default_standby_max_watt")
    private BigDecimal defaultStandbyMaxWatt;

    @Column(name = "supports_standby", nullable = false)
    private boolean supportsStandby;

    @Column(name = "supports_schedule", nullable = false)
    private boolean supportsSchedule;

    @Column(name = "supports_operating_modes", nullable = false)
    private boolean supportsOperatingModes;

    @Column(name = "icon_key", nullable = false)
    private String iconKey;

    @Column(name = "search_keywords")
    private String searchKeywords;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "catalog_version", nullable = false)
    private int catalogVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApplianceCatalogEntity() {
    }

    ApplianceCatalogEntity(UUID id, String code, String displayName, String description, String category,
                            String behaviorProfile, BigDecimal defaultSafePowerLimitWatt,
                            BigDecimal defaultActiveMinWatt, BigDecimal defaultActiveMaxWatt,
                            BigDecimal defaultStandbyMinWatt, BigDecimal defaultStandbyMaxWatt,
                            boolean supportsStandby, boolean supportsSchedule, boolean supportsOperatingModes,
                            String iconKey, String searchKeywords, boolean featured, boolean enabled,
                            int displayOrder, int catalogVersion, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.behaviorProfile = behaviorProfile;
        this.defaultSafePowerLimitWatt = defaultSafePowerLimitWatt;
        this.defaultActiveMinWatt = defaultActiveMinWatt;
        this.defaultActiveMaxWatt = defaultActiveMaxWatt;
        this.defaultStandbyMinWatt = defaultStandbyMinWatt;
        this.defaultStandbyMaxWatt = defaultStandbyMaxWatt;
        this.supportsStandby = supportsStandby;
        this.supportsSchedule = supportsSchedule;
        this.supportsOperatingModes = supportsOperatingModes;
        this.iconKey = iconKey;
        this.searchKeywords = searchKeywords;
        this.featured = featured;
        this.enabled = enabled;
        this.displayOrder = displayOrder;
        this.catalogVersion = catalogVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getDisplayName() {
        return displayName;
    }

    String getDescription() {
        return description;
    }

    String getCategory() {
        return category;
    }

    String getBehaviorProfile() {
        return behaviorProfile;
    }

    BigDecimal getDefaultSafePowerLimitWatt() {
        return defaultSafePowerLimitWatt;
    }

    BigDecimal getDefaultActiveMinWatt() {
        return defaultActiveMinWatt;
    }

    BigDecimal getDefaultActiveMaxWatt() {
        return defaultActiveMaxWatt;
    }

    BigDecimal getDefaultStandbyMinWatt() {
        return defaultStandbyMinWatt;
    }

    BigDecimal getDefaultStandbyMaxWatt() {
        return defaultStandbyMaxWatt;
    }

    boolean isSupportsStandby() {
        return supportsStandby;
    }

    boolean isSupportsSchedule() {
        return supportsSchedule;
    }

    boolean isSupportsOperatingModes() {
        return supportsOperatingModes;
    }

    String getIconKey() {
        return iconKey;
    }

    String getSearchKeywords() {
        return searchKeywords;
    }

    boolean isFeatured() {
        return featured;
    }

    boolean isEnabled() {
        return enabled;
    }

    int getDisplayOrder() {
        return displayOrder;
    }

    int getCatalogVersion() {
        return catalogVersion;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
