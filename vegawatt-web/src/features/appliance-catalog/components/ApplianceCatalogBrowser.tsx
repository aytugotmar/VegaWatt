import { useMemo, useState } from "react";
import { useApplianceCatalogQuery } from "../../../shared/hooks/useApplianceCatalog";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import type { CustomAppliance, FieldErrors } from "../../home-registration/registrationSchema";
import { useDebouncedValue } from "../../../shared/hooks/useDebouncedValue";
import type { SelectedCatalogAppliance, WattField } from "../hooks/useCatalogSelection";
import { FEATURED_TAB, filterCatalogItems, type ApplianceCatalogTab } from "../model/applianceCatalogFilters";
import { ApplianceCatalogDetailsDrawer } from "./ApplianceCatalogDetailsDrawer";
import { ApplianceCatalogGrid } from "./ApplianceCatalogGrid";
import { ApplianceCatalogSearch } from "./ApplianceCatalogSearch";
import { ApplianceCategoryTabs } from "./ApplianceCategoryTabs";
import { CustomApplianceForm } from "./CustomApplianceForm";
import { SelectedApplianceTray } from "./SelectedApplianceTray";

const SEARCH_DEBOUNCE_MS = 250;

interface ApplianceCatalogBrowserProps {
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

/** Orchestrates the catalog browse/search/select experience — owns its own data fetch so loading
 * and error state no longer leak up into `AddHomeWizard` (a simplification vs. Aşama 9). */
export function ApplianceCatalogBrowser({
  selectedAppliances,
  selectionErrors,
  onAddInstance,
  onDecrement,
  onRename,
  onUpdateOverride,
  customAppliances,
  customErrors,
  onAddCustom,
  onRemoveCustom,
  onCustomChange,
  hasAtLeastOneAppliance,
}: ApplianceCatalogBrowserProps) {
  const catalogQuery = useApplianceCatalogQuery();
  const catalogItems = useMemo(() => catalogQuery.data ?? [], [catalogQuery.data]);
  const catalogById = useMemo(() => new Map(catalogItems.map((item) => [item.id, item])), [catalogItems]);

  const [activeTab, setActiveTab] = useState<ApplianceCatalogTab>(FEATURED_TAB);
  const [searchQuery, setSearchQuery] = useState("");
  const debouncedQuery = useDebouncedValue(searchQuery, SEARCH_DEBOUNCE_MS);
  const [detailsItem, setDetailsItem] = useState<ApplianceCatalogItem | null>(null);

  const filteredItems = useMemo(
    () => filterCatalogItems(catalogItems, { tab: activeTab, query: debouncedQuery }),
    [catalogItems, activeTab, debouncedQuery],
  );

  function resolveAndIncrement(catalogItemId: string) {
    const item = catalogById.get(catalogItemId);
    if (item) onAddInstance(item);
  }

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h3 className="mb-1 text-sm font-semibold text-text-primary">Cihazlar</h3>
        <p className="mb-3 text-xs text-text-secondary">Birden fazla adet seçebilirsiniz, örn. 3 buzdolabı.</p>

        {!catalogQuery.isLoading && !catalogQuery.isError && catalogItems.length > 0 && (
          <div className="mb-3 flex flex-col gap-2">
            <ApplianceCatalogSearch value={searchQuery} onChange={setSearchQuery} />
            <ApplianceCategoryTabs active={activeTab} onChange={setActiveTab} />
          </div>
        )}

        <ApplianceCatalogGrid
          items={filteredItems}
          hasAnyCatalogItems={catalogItems.length > 0}
          loading={catalogQuery.isLoading}
          error={catalogQuery.isError}
          onRetry={() => catalogQuery.refetch()}
          searchQuery={debouncedQuery}
          onAddCustom={onAddCustom}
          onOpenDetails={setDetailsItem}
          onAdd={onAddInstance}
        />

        {!hasAtLeastOneAppliance && (
          <p className="mt-2 text-xs font-medium text-danger">En az bir cihaz seçmeli veya eklemelisiniz.</p>
        )}
      </div>

      <SelectedApplianceTray
        selectedAppliances={selectedAppliances}
        errors={selectionErrors}
        onIncrement={resolveAndIncrement}
        onDecrement={onDecrement}
        onRename={onRename}
        onUpdateOverride={onUpdateOverride}
      />

      <CustomApplianceForm
        customAppliances={customAppliances}
        customErrors={customErrors}
        onAddCustom={onAddCustom}
        onRemoveCustom={onRemoveCustom}
        onCustomChange={onCustomChange}
      />

      {detailsItem && (
        <ApplianceCatalogDetailsDrawer
          item={detailsItem}
          onClose={() => setDetailsItem(null)}
          onAdd={() => {
            onAddInstance(detailsItem);
            setDetailsItem(null);
          }}
        />
      )}
    </div>
  );
}

