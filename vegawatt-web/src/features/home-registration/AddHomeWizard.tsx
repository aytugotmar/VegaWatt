import { X } from "lucide-react";
import { useId, useMemo, useState } from "react";
import { Dialog } from "../../shared/components/Dialog";
import { Button } from "../../shared/components/Button";
import { ApiError } from "../../shared/api/client";
import { useRegisterHomeMutation } from "../../shared/hooks/useHomesQueries";
import { useCatalogSelection } from "../appliance-catalog/hooks/useCatalogSelection";
import { buildApplianceRegistrations } from "../appliance-catalog/model/buildApplianceRegistrations";
import { AppliancesStep } from "./AppliancesStep";
import { HomeInfoStep } from "./HomeInfoStep";
import {
  newCustomAppliance,
  validateCustomAppliance,
  validateHomeInfo,
  validateTargets,
  type CustomAppliance,
  type FieldErrors,
  type HomeInfoValues,
  type TargetsValues,
} from "./registrationSchema";
import { ReviewStep } from "./ReviewStep";
import { TargetsStep } from "./TargetsStep";

interface AddHomeWizardProps {
  onClose: () => void;
}

const STEP_LABELS = ["Ev bilgileri", "Hedefler", "Cihazlar", "Kontrol"];

const INITIAL_HOME_INFO: HomeInfoValues = { name: "", contactEmail: "" };
const INITIAL_TARGETS: TargetsValues = {
  energyQuotaKwh: "300",
  budgetQuotaTry: "1500",
  baseTariffPerKwh: "2.10",
  penaltyTariffPerKwh: "3.50",
};

export function AddHomeWizard({ onClose }: AddHomeWizardProps) {
  const titleId = useId();
  const [step, setStep] = useState(0);
  const [homeInfo, setHomeInfo] = useState(INITIAL_HOME_INFO);
  const [targets, setTargets] = useState(INITIAL_TARGETS);
  const [customAppliances, setCustomAppliances] = useState<CustomAppliance[]>([]);
  const [attemptedSteps, setAttemptedSteps] = useState<Set<number>>(new Set());

  const registerMutation = useRegisterHomeMutation();
  const catalogSelection = useCatalogSelection();

  const rawHomeInfoErrors = validateHomeInfo(homeInfo);
  const rawTargetsErrors = validateTargets(targets);
  const homeInfoErrors = attemptedSteps.has(0) ? rawHomeInfoErrors : {};
  const targetsErrors = attemptedSteps.has(1) ? rawTargetsErrors : {};
  const showApplianceErrors = attemptedSteps.has(2);

  const customErrors = useMemo(() => {
    const result: Record<number, FieldErrors> = {};
    for (const appliance of customAppliances) {
      result[appliance.id] = validateCustomAppliance(appliance);
    }
    return result;
  }, [customAppliances]);

  const hasAtLeastOneAppliance = catalogSelection.selectedAppliances.length > 0 || customAppliances.length > 0;

  const appliancesValid =
    hasAtLeastOneAppliance &&
    Object.values(catalogSelection.errors).every((errors) => Object.keys(errors).length === 0) &&
    Object.values(customErrors).every((errors) => Object.keys(errors).length === 0);

  function buildAppliances() {
    return buildApplianceRegistrations(catalogSelection.selectedAppliances, customAppliances);
  }

  function goNext() {
    setAttemptedSteps((current) => new Set(current).add(step));
    if (step === 0 && Object.keys(rawHomeInfoErrors).length > 0) return;
    if (step === 1 && Object.keys(rawTargetsErrors).length > 0) return;
    if (step === 2 && !appliancesValid) return;
    setStep((current) => Math.min(current + 1, STEP_LABELS.length - 1));
  }

  function goBack() {
    setStep((current) => Math.max(current - 1, 0));
  }

  function handleSubmit() {
    registerMutation.mutate({
      name: homeInfo.name,
      contactEmail: homeInfo.contactEmail,
      energyQuotaKwh: Number(targets.energyQuotaKwh),
      budgetQuotaTry: Number(targets.budgetQuotaTry),
      baseTariffPerKwh: Number(targets.baseTariffPerKwh),
      penaltyTariffPerKwh: Number(targets.penaltyTariffPerKwh),
      appliances: buildAppliances(),
    });
  }

  const errorMessage = registerMutation.isError
    ? registerMutation.error instanceof ApiError
      ? registerMutation.error.message
      : "Ev kaydedilirken bir hata oluştu."
    : null;

  return (
    <Dialog open onClose={onClose} labelledBy={titleId} maxWidthClassName="max-w-2xl">
      <div className="flex shrink-0 items-center justify-between gap-3 border-b border-border px-5 py-4">
        <h2 id={titleId} className="text-lg font-semibold text-text-primary">
          Yeni Ev Ekle
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

      {!registerMutation.isSuccess && (
        <nav className="flex shrink-0 items-center gap-2 border-b border-border px-5 py-3 overflow-x-auto max-w-full min-w-0" aria-label="Kayıt adımları">
          {STEP_LABELS.map((label, index) => (
            <div key={label} className="flex shrink-0 items-center gap-2">
              <span
                className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold ${
                  index === step
                    ? "bg-primary text-white"
                    : index < step
                      ? "bg-primary-soft text-primary"
                      : "bg-surface-subtle text-text-muted"
                }`}
              >
                {index + 1}
              </span>
              <span className={`text-xs font-medium whitespace-nowrap ${index === step ? "text-text-primary" : "text-text-muted"}`}>
                {label}
              </span>
              {index < STEP_LABELS.length - 1 && <span className="mx-1 h-px w-4 bg-border shrink-0" aria-hidden="true" />}
            </div>
          ))}
        </nav>
      )}

      <div className="flex-1 min-h-0 overflow-y-auto px-5 py-4">
        {step === 0 && (
          <HomeInfoStep
            values={homeInfo}
            errors={homeInfoErrors}
            onChange={(field, value) => setHomeInfo((current) => ({ ...current, [field]: value }))}
          />
        )}
        {step === 1 && (
          <TargetsStep
            values={targets}
            errors={targetsErrors}
            onChange={(field, value) => setTargets((current) => ({ ...current, [field]: value }))}
          />
        )}
        {step === 2 && (
          <AppliancesStep
            selectedAppliances={catalogSelection.selectedAppliances}
            selectionErrors={showApplianceErrors ? catalogSelection.errors : {}}
            onAddInstance={catalogSelection.addInstance}
            onDecrement={catalogSelection.decrementQuantity}
            onRename={catalogSelection.renameInstance}
            onUpdateOverride={catalogSelection.updateOverride}
            customAppliances={customAppliances}
            customErrors={showApplianceErrors ? customErrors : {}}
            onAddCustom={() => setCustomAppliances((current) => [newCustomAppliance(), ...current])}
            onRemoveCustom={(id) => setCustomAppliances((current) => current.filter((a) => a.id !== id))}
            onCustomChange={(id, field, value) =>
              setCustomAppliances((current) =>
                current.map((appliance) => (appliance.id === id ? { ...appliance, [field]: value } : appliance)),
              )
            }
            hasAtLeastOneAppliance={!showApplianceErrors || hasAtLeastOneAppliance}
          />
        )}
        {step === 3 && (
          <ReviewStep
            homeInfo={homeInfo}
            targets={targets}
            appliances={buildAppliances()}
            isPending={registerMutation.isPending}
            isSuccess={registerMutation.isSuccess}
            errorMessage={errorMessage}
            onSubmit={handleSubmit}
            onClose={onClose}
          />
        )}
      </div>

      {!registerMutation.isSuccess && (
        <div className="flex shrink-0 items-center justify-between gap-2 border-t border-border px-5 py-3 bg-surface z-10">
          <Button variant="ghost" onClick={goBack} disabled={step === 0}>
            Geri
          </Button>
          {step < STEP_LABELS.length - 1 && (
            <Button variant="primary" onClick={goNext}>
              İleri
            </Button>
          )}
        </div>
      )}
    </Dialog>
  );
}
