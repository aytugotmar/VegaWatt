import { X } from "lucide-react";
import { useId, useMemo, useState } from "react";
import { Dialog } from "../../shared/components/Dialog";
import { Button } from "../../shared/components/Button";
import { ApiError } from "../../shared/api/client";
import { APPLIANCE_TYPE_PRESETS } from "../../shared/constants/applianceTypes";
import { useRegisterHomeMutation } from "../../shared/hooks/useHomesQueries";
import type { ApplianceRegistration } from "../../shared/types/home";
import { AppliancesStep } from "./AppliancesStep";
import { HomeInfoStep } from "./HomeInfoStep";
import {
  validateApplianceLimits,
  validateCustomAppliance,
  validateHomeInfo,
  validateTargets,
  type CustomAppliance,
  type FieldErrors,
  type HomeInfoValues,
  type PresetSelection,
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

function initialSelections(): Record<string, PresetSelection> {
  return Object.fromEntries(
    APPLIANCE_TYPE_PRESETS.map((preset) => [
      preset.key,
      {
        checked: false,
        quantity: 1,
        customized: false,
        safePowerLimitWatt: String(preset.safePowerLimitWatt),
        simulationMinWatt: String(preset.simulationMinWatt),
        simulationMaxWatt: String(preset.simulationMaxWatt),
      },
    ]),
  );
}

let customIdSeq = 0;
function newCustomAppliance(): CustomAppliance {
  return {
    id: customIdSeq++,
    name: "",
    type: "OTHER",
    safePowerLimitWatt: "500",
    simulationMinWatt: "100",
    simulationMaxWatt: "500",
  };
}

export function AddHomeWizard({ onClose }: AddHomeWizardProps) {
  const titleId = useId();
  const [step, setStep] = useState(0);
  const [homeInfo, setHomeInfo] = useState(INITIAL_HOME_INFO);
  const [targets, setTargets] = useState(INITIAL_TARGETS);
  const [selections, setSelections] = useState<Record<string, PresetSelection>>(() => initialSelections());
  const [customAppliances, setCustomAppliances] = useState<CustomAppliance[]>([]);
  const [attemptedSteps, setAttemptedSteps] = useState<Set<number>>(new Set());

  const registerMutation = useRegisterHomeMutation();

  const rawHomeInfoErrors = validateHomeInfo(homeInfo);
  const rawTargetsErrors = validateTargets(targets);
  const homeInfoErrors = attemptedSteps.has(0) ? rawHomeInfoErrors : {};
  const targetsErrors = attemptedSteps.has(1) ? rawTargetsErrors : {};
  const showApplianceErrors = attemptedSteps.has(2);

  const presetErrors = useMemo(() => {
    const result: Record<string, FieldErrors> = {};
    for (const preset of APPLIANCE_TYPE_PRESETS) {
      const selection = selections[preset.key];
      if (selection.checked) {
        result[preset.key] = validateApplianceLimits(selection);
      }
    }
    return result;
  }, [selections]);

  const customErrors = useMemo(() => {
    const result: Record<number, FieldErrors> = {};
    for (const appliance of customAppliances) {
      result[appliance.id] = validateCustomAppliance(appliance);
    }
    return result;
  }, [customAppliances]);

  const hasAtLeastOneAppliance =
    Object.values(selections).some((selection) => selection.checked) || customAppliances.length > 0;

  const appliancesValid =
    hasAtLeastOneAppliance &&
    Object.values(presetErrors).every((errors) => Object.keys(errors).length === 0) &&
    Object.values(customErrors).every((errors) => Object.keys(errors).length === 0);

  function buildAppliances(): ApplianceRegistration[] {
    const fromPresets = APPLIANCE_TYPE_PRESETS.flatMap((preset) => {
      const selection = selections[preset.key];
      if (!selection.checked) return [];
      return Array.from({ length: selection.quantity }, (_, index) => ({
        name: selection.quantity === 1 ? preset.label : `${preset.label} ${index + 1}`,
        type: preset.type,
        safePowerLimitWatt: Number(selection.safePowerLimitWatt),
        simulationMinWatt: Number(selection.simulationMinWatt),
        simulationMaxWatt: Number(selection.simulationMaxWatt),
      }));
    });

    const fromCustom = customAppliances.map((appliance) => ({
      name: appliance.name,
      type: appliance.type,
      safePowerLimitWatt: Number(appliance.safePowerLimitWatt),
      simulationMinWatt: Number(appliance.simulationMinWatt),
      simulationMaxWatt: Number(appliance.simulationMaxWatt),
    }));

    return [...fromPresets, ...fromCustom];
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
      <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4">
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
        <nav className="flex items-center gap-2 border-b border-border px-5 py-3" aria-label="Kayıt adımları">
          {STEP_LABELS.map((label, index) => (
            <div key={label} className="flex items-center gap-2">
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
              <span className={`text-xs font-medium ${index === step ? "text-text-primary" : "text-text-muted"}`}>
                {label}
              </span>
              {index < STEP_LABELS.length - 1 && <span className="mx-1 h-px w-4 bg-border" aria-hidden="true" />}
            </div>
          ))}
        </nav>
      )}

      <div className="flex-1 overflow-y-auto px-5 py-4">
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
            selections={selections}
            presetErrors={showApplianceErrors ? presetErrors : {}}
            onTogglePreset={(key) =>
              setSelections((current) => ({ ...current, [key]: { ...current[key], checked: !current[key].checked } }))
            }
            onQuantityChange={(key, delta) =>
              setSelections((current) => ({
                ...current,
                [key]: { ...current[key], quantity: Math.min(10, Math.max(1, current[key].quantity + delta)) },
              }))
            }
            onToggleCustomize={(key) =>
              setSelections((current) => ({
                ...current,
                [key]: { ...current[key], customized: !current[key].customized },
              }))
            }
            onLimitChange={(key, field, value) =>
              setSelections((current) => ({ ...current, [key]: { ...current[key], [field]: value } }))
            }
            customAppliances={customAppliances}
            customErrors={showApplianceErrors ? customErrors : {}}
            onAddCustom={() => setCustomAppliances((current) => [...current, newCustomAppliance()])}
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
        <div className="flex items-center justify-between gap-2 border-t border-border px-5 py-3">
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
