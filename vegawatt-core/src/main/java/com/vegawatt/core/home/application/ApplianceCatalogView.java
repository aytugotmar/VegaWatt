package com.vegawatt.core.home.application;

/** Cosmetic catalog info resolved live (not from the appliance's snapshot) for the live-status API
 * response — display name/icon should reflect the current catalog entry, unlike the
 * simulation-critical snapshot fields on {@link com.vegawatt.core.home.domain.Appliance}. */
public record ApplianceCatalogView(String catalogCode, String catalogDisplayName, String catalogIconKey) {

    public static final ApplianceCatalogView EMPTY = new ApplianceCatalogView(null, null, null);
}
