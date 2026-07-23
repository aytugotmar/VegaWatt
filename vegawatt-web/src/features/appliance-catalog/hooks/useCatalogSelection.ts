import { useMemo, useState } from "react";
import { validateApplianceLimits, type FieldErrors } from "../../home-registration/registrationSchema";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";

export type WattField = "safePowerLimitWatt" | "simulationMinWatt" | "simulationMaxWatt";

/** One selected appliance instance — self-contained (snapshots the catalog item's defaults at
 * add-time) so validation/submission never need to look the catalog item back up, and the catalog
 * item itself is never mutated (yapılacak.md §20.6). `overridden` extends the spec's
 * `SelectedCatalogAppliance` shape: an untouched default must never be sent to the backend as an
 * override (established in Aşama 9 — the backend applies its own catalog default when a field is
 * `null`). */
export interface SelectedCatalogAppliance {
  instanceKey: string;
  catalogItemId: string;
  catalogCode: string;
  catalogDisplayName: string;
  catalogIconKey: string;
  catalogBehaviorProfile: string;
  defaultSafePowerLimitWatt: number;
  defaultSimulationMinWatt: number;
  defaultSimulationMaxWatt: number;
  defaultStandbyMinWatt: number | null;
  defaultStandbyMaxWatt: number | null;
  name: string;
  safePowerLimitWatt: string;
  simulationMinWatt: string;
  simulationMaxWatt: string;
  overridden: Record<WattField, boolean>;
}

function newInstanceKey(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `instance-${Math.random().toString(36).slice(2)}`;
}

function toInstance(item: ApplianceCatalogItem, ordinal: number): SelectedCatalogAppliance {
  return {
    instanceKey: newInstanceKey(),
    catalogItemId: item.id,
    catalogCode: item.code,
    catalogDisplayName: item.displayName,
    catalogIconKey: item.iconKey,
    catalogBehaviorProfile: item.behaviorProfile,
    defaultSafePowerLimitWatt: item.defaultSafePowerLimitWatt,
    defaultSimulationMinWatt: item.defaultActiveMinWatt,
    defaultSimulationMaxWatt: item.defaultActiveMaxWatt,
    defaultStandbyMinWatt: item.defaultStandbyMinWatt,
    defaultStandbyMaxWatt: item.defaultStandbyMaxWatt,
    name: ordinal === 1 ? item.displayName : `${item.displayName} ${ordinal}`,
    safePowerLimitWatt: String(item.defaultSafePowerLimitWatt),
    simulationMinWatt: String(item.defaultActiveMinWatt),
    simulationMaxWatt: String(item.defaultActiveMaxWatt),
    overridden: { safePowerLimitWatt: false, simulationMinWatt: false, simulationMaxWatt: false },
  };
}

export function useCatalogSelection() {
  const [selectedAppliances, setSelectedAppliances] = useState<SelectedCatalogAppliance[]>([]);

  function quantityFor(catalogItemId: string): number {
    return selectedAppliances.filter((instance) => instance.catalogItemId === catalogItemId).length;
  }

  function addInstance(item: ApplianceCatalogItem) {
    setSelectedAppliances((current) => {
      const existingCount = current.filter((instance) => instance.catalogItemId === item.id).length;
      return [...current, toInstance(item, existingCount + 1)];
    });
  }

  function removeInstance(instanceKey: string) {
    setSelectedAppliances((current) => current.filter((instance) => instance.instanceKey !== instanceKey));
  }

  /** Removes the most recently added instance of this catalog item (mirrors the tray's "-"
   * stepper, which operates on the group as a whole rather than a specific instance). */
  function decrementQuantity(catalogItemId: string) {
    setSelectedAppliances((current) => {
      const lastIndex = current.map((instance) => instance.catalogItemId).lastIndexOf(catalogItemId);
      if (lastIndex === -1) return current;
      return [...current.slice(0, lastIndex), ...current.slice(lastIndex + 1)];
    });
  }

  function renameInstance(instanceKey: string, name: string) {
    setSelectedAppliances((current) =>
      current.map((instance) => (instance.instanceKey === instanceKey ? { ...instance, name } : instance)),
    );
  }

  function updateOverride(instanceKey: string, field: WattField, value: string) {
    setSelectedAppliances((current) =>
      current.map((instance) =>
        instance.instanceKey === instanceKey
          ? { ...instance, [field]: value, overridden: { ...instance.overridden, [field]: true } }
          : instance,
      ),
    );
  }

  const errors = useMemo(() => {
    const result: Record<string, FieldErrors> = {};
    for (const instance of selectedAppliances) {
      const effective = {
        safePowerLimitWatt: instance.overridden.safePowerLimitWatt
          ? instance.safePowerLimitWatt
          : String(instance.defaultSafePowerLimitWatt),
        simulationMinWatt: instance.overridden.simulationMinWatt
          ? instance.simulationMinWatt
          : String(instance.defaultSimulationMinWatt),
        simulationMaxWatt: instance.overridden.simulationMaxWatt
          ? instance.simulationMaxWatt
          : String(instance.defaultSimulationMaxWatt),
      };
      const allErrors = validateApplianceLimits(effective);
      const instanceErrors: FieldErrors = {};
      (Object.keys(instance.overridden) as WattField[]).forEach((field) => {
        if (instance.overridden[field] && allErrors[field]) {
          instanceErrors[field] = allErrors[field];
        }
      });
      if (!instance.name.trim()) {
        instanceErrors.name = "Cihaz adı zorunludur.";
      }
      if (Object.keys(instanceErrors).length > 0) {
        result[instance.instanceKey] = instanceErrors;
      }
    }
    return result;
  }, [selectedAppliances]);

  return {
    selectedAppliances,
    quantityFor,
    addInstance,
    removeInstance,
    decrementQuantity,
    renameInstance,
    updateOverride,
    errors,
  };
}
