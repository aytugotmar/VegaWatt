import type { ApplianceRegistration } from "../../../shared/types/home";
import type { CustomAppliance } from "../../home-registration/registrationSchema";
import type { SelectedCatalogAppliance } from "../hooks/useCatalogSelection";

/** Turns catalog selections and custom appliances into the request shape both home registration
 * and the "add a device later" flow submit to the backend. */
export function buildApplianceRegistrations(
  selectedAppliances: SelectedCatalogAppliance[],
  customAppliances: CustomAppliance[],
): ApplianceRegistration[] {
  const fromCatalog = selectedAppliances.map((instance) => ({
    name: instance.name,
    type: instance.catalogCode,
    catalogItemId: instance.catalogItemId,
    safePowerLimitWatt: instance.overridden.safePowerLimitWatt ? Number(instance.safePowerLimitWatt) : null,
    simulationMinWatt: instance.overridden.simulationMinWatt ? Number(instance.simulationMinWatt) : null,
    simulationMaxWatt: instance.overridden.simulationMaxWatt ? Number(instance.simulationMaxWatt) : null,
  }));

  const fromCustom = customAppliances.map((appliance) => ({
    name: appliance.name,
    type: appliance.type,
    catalogItemId: null,
    safePowerLimitWatt: Number(appliance.safePowerLimitWatt),
    simulationMinWatt: Number(appliance.simulationMinWatt),
    simulationMaxWatt: Number(appliance.simulationMaxWatt),
  }));

  return [...fromCatalog, ...fromCustom];
}
