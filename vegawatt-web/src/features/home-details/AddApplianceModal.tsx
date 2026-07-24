import { X } from "lucide-react";
import { useId, useMemo, useState } from "react";
import { ApiError } from "../../shared/api/client";
import { Button } from "../../shared/components/Button";
import { Dialog } from "../../shared/components/Dialog";
import { useAddAppliancesMutation } from "../../shared/hooks/useHomesQueries";
import { useCatalogSelection } from "../appliance-catalog/hooks/useCatalogSelection";
import { buildApplianceRegistrations } from "../appliance-catalog/model/buildApplianceRegistrations";
import { AppliancesStep } from "../home-registration/AppliancesStep";
import {
  newCustomAppliance,
  validateCustomAppliance,
  type CustomAppliance,
  type FieldErrors,
} from "../home-registration/registrationSchema";

interface AddApplianceModalProps {
  homeId: string;
  onClose: () => void;
}

/** Lets a user add a forgotten device to a home they already registered — reuses the same
 * catalog browse/select experience as the registration wizard's appliances step, just scoped
 * to one home and submitted immediately instead of deferred to a final review step. */
export function AddApplianceModal({ homeId, onClose }: AddApplianceModalProps) {
  const titleId = useId();
  const [customAppliances, setCustomAppliances] = useState<CustomAppliance[]>([]);
  const [attempted, setAttempted] = useState(false);

  const catalogSelection = useCatalogSelection();
  const addAppliancesMutation = useAddAppliancesMutation(homeId);

  const customErrors = useMemo(() => {
    const result: Record<number, FieldErrors> = {};
    for (const appliance of customAppliances) {
      result[appliance.id] = validateCustomAppliance(appliance);
    }
    return result;
  }, [customAppliances]);

  const hasAtLeastOneAppliance = catalogSelection.selectedAppliances.length > 0 || customAppliances.length > 0;
  const isValid =
    hasAtLeastOneAppliance &&
    Object.values(catalogSelection.errors).every((errors) => Object.keys(errors).length === 0) &&
    Object.values(customErrors).every((errors) => Object.keys(errors).length === 0);

  function handleSubmit() {
    setAttempted(true);
    if (!isValid) return;
    addAppliancesMutation.mutate(buildApplianceRegistrations(catalogSelection.selectedAppliances, customAppliances), {
      onSuccess: onClose,
    });
  }

  const errorMessage = addAppliancesMutation.isError
    ? addAppliancesMutation.error instanceof ApiError
      ? addAppliancesMutation.error.message
      : "Cihaz eklenirken bir hata oluştu."
    : null;

  return (
    <Dialog open onClose={onClose} labelledBy={titleId} maxWidthClassName="max-w-4xl">
      <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4">
        <h2 id={titleId} className="text-lg font-semibold text-text-primary">
          Cihaz Ekle
        </h2>
        <button
          type="button"
          onClick={onClose}
          aria-label="Kapat"
          className="flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition hover:bg-surface-subtle hover:text-text-primary"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4">
        <AppliancesStep
          selectedAppliances={catalogSelection.selectedAppliances}
          selectionErrors={attempted ? catalogSelection.errors : {}}
          onAddInstance={catalogSelection.addInstance}
          onDecrement={catalogSelection.decrementQuantity}
          onRename={catalogSelection.renameInstance}
          onUpdateOverride={catalogSelection.updateOverride}
          customAppliances={customAppliances}
          customErrors={attempted ? customErrors : {}}
          onAddCustom={() => setCustomAppliances((current) => [...current, newCustomAppliance()])}
          onRemoveCustom={(id) => setCustomAppliances((current) => current.filter((appliance) => appliance.id !== id))}
          onCustomChange={(id, field, value) =>
            setCustomAppliances((current) =>
              current.map((appliance) => (appliance.id === id ? { ...appliance, [field]: value } : appliance)),
            )
          }
          hasAtLeastOneAppliance={!attempted || hasAtLeastOneAppliance}
        />

        {errorMessage && (
          <p className="mt-3 rounded-input border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
            {errorMessage}
          </p>
        )}
      </div>

      <div className="flex items-center justify-end gap-2 border-t border-border px-5 py-3">
        <Button variant="ghost" onClick={onClose} disabled={addAppliancesMutation.isPending}>
          İptal
        </Button>
        <Button variant="primary" onClick={handleSubmit} loading={addAppliancesMutation.isPending}>
          {addAppliancesMutation.isPending ? "Ekleniyor…" : "Cihazları Ekle"}
        </Button>
      </div>
    </Dialog>
  );
}
