import { ApplianceCatalogBrowser } from "../appliance-catalog/components/ApplianceCatalogBrowser";
import type { SelectedCatalogAppliance, WattField } from "../appliance-catalog/hooks/useCatalogSelection";
import type { ApplianceCatalogItem } from "../../shared/types/applianceCatalog";
import type { CustomAppliance, FieldErrors } from "./registrationSchema";

interface AppliancesStepProps {
  selectedAppliances: SelectedCatalogAppliance[];
  selectionErrors: Record<string, FieldErrors>;
  onAddInstance: (item: ApplianceCatalogItem) => void;
  onDecrement: (catalogItemId: string) => void;
  onRename: (instanceKey: string, name: string) => void;
  onUpdateOverride: (instanceKey: string, field: WattField, value: string) => void;
  customAppliances: CustomAppliance[];
  customErrors: Record<number, FieldErrors>;
  onAddCustom: () => void;
  onRemoveCustom: (id: number) => void;
  onCustomChange: (id: number, field: keyof Omit<CustomAppliance, "id">, value: string) => void;
  hasAtLeastOneAppliance: boolean;
}

export function AppliancesStep(props: AppliancesStepProps) {
  return <ApplianceCatalogBrowser {...props} />;
}
