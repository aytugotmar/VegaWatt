import { useState } from "react";
import type { FieldErrors } from "../../home-registration/registrationSchema";
import type { SelectedCatalogAppliance, WattField } from "../hooks/useCatalogSelection";
import { SelectedApplianceItem } from "./SelectedApplianceItem";

interface ApplianceGroup {
  catalogItemId: string;
  catalogDisplayName: string;
  catalogIconKey: string;
  instances: SelectedCatalogAppliance[];
}

function groupByCatalogItem(selectedAppliances: SelectedCatalogAppliance[]): ApplianceGroup[] {
  const groups: ApplianceGroup[] = [];
  const indexByCatalogItemId = new Map<string, number>();

  for (const instance of selectedAppliances) {
    const existingIndex = indexByCatalogItemId.get(instance.catalogItemId);
    if (existingIndex === undefined) {
      indexByCatalogItemId.set(instance.catalogItemId, groups.length);
      groups.push({
        catalogItemId: instance.catalogItemId,
        catalogDisplayName: instance.catalogDisplayName,
        catalogIconKey: instance.catalogIconKey,
        instances: [instance],
      });
    } else {
      groups[existingIndex].instances.push(instance);
    }
  }

  return groups;
}

interface SelectedApplianceTrayProps {
  selectedAppliances: SelectedCatalogAppliance[];
  errors: Record<string, FieldErrors>;
  onIncrement: (catalogItemId: string) => void;
  onDecrement: (catalogItemId: string) => void;
  onRename: (instanceKey: string, name: string) => void;
  onUpdateOverride: (instanceKey: string, field: WattField, value: string) => void;
}

/** Groups selected instances by catalog item for display only — the underlying state in
 * `useCatalogSelection` stays a flat instance list (yapılacak.md §20.6 mockup: "Klima  1"). */
export function SelectedApplianceTray({
  selectedAppliances,
  errors,
  onIncrement,
  onDecrement,
  onRename,
  onUpdateOverride,
}: SelectedApplianceTrayProps) {
  const [expandedCatalogItemId, setExpandedCatalogItemId] = useState<string | null>(null);

  if (selectedAppliances.length === 0) {
    return (
      <p className="rounded-card border border-dashed border-border p-3 text-xs text-text-muted">
        Henüz cihaz seçmediniz. Yukarıdaki katalogdan bir cihaz ekleyin.
      </p>
    );
  }

  const groups = groupByCatalogItem(selectedAppliances);

  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-xs font-semibold uppercase tracking-wide text-text-secondary">
        Seçilen Cihazlar ({selectedAppliances.length})
      </h3>
      {groups.map((group) => (
        <SelectedApplianceItem
          key={group.catalogItemId}
          catalogDisplayName={group.catalogDisplayName}
          catalogIconKey={group.catalogIconKey}
          instances={group.instances}
          expanded={expandedCatalogItemId === group.catalogItemId}
          onToggleExpanded={() =>
            setExpandedCatalogItemId((current) => (current === group.catalogItemId ? null : group.catalogItemId))
          }
          errors={errors}
          onIncrement={() => onIncrement(group.catalogItemId)}
          onDecrement={() => onDecrement(group.catalogItemId)}
          onRename={onRename}
          onUpdateOverride={onUpdateOverride}
        />
      ))}
    </div>
  );
}
