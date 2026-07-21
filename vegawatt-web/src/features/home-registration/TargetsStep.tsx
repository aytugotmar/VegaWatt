import { ChevronDown } from "lucide-react";
import { useState } from "react";
import { Input } from "../../shared/components/Input";
import type { FieldErrors, TargetsValues } from "./registrationSchema";

interface TargetsStepProps {
  values: TargetsValues;
  errors: FieldErrors;
  onChange: (field: keyof TargetsValues, value: string) => void;
}

export function TargetsStep({ values, errors, onChange }: TargetsStepProps) {
  const [advancedOpen, setAdvancedOpen] = useState(false);

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Input
          label="Enerji hedefi (kWh)"
          type="number"
          min="0"
          step="0.01"
          value={values.energyQuotaKwh}
          onChange={(event) => onChange("energyQuotaKwh", event.target.value)}
          error={errors.energyQuotaKwh}
        />
        <Input
          label="Bütçe hedefi (TRY)"
          type="number"
          min="0"
          step="0.01"
          value={values.budgetQuotaTry}
          onChange={(event) => onChange("budgetQuotaTry", event.target.value)}
          error={errors.budgetQuotaTry}
        />
      </div>

      <div className="rounded-input border border-border">
        <button
          type="button"
          onClick={() => setAdvancedOpen((current) => !current)}
          aria-expanded={advancedOpen}
          className="flex w-full items-center justify-between px-3 py-2.5 text-sm font-medium text-text-primary"
        >
          Gelişmiş tarife ayarları
          <ChevronDown className={`h-4 w-4 text-text-muted transition ${advancedOpen ? "rotate-180" : ""}`} aria-hidden="true" />
        </button>
        {advancedOpen && (
          <div className="grid grid-cols-1 gap-4 border-t border-border p-3 sm:grid-cols-2">
            <Input
              label="Baz tarife (TRY/kWh)"
              type="number"
              min="0"
              step="0.01"
              value={values.baseTariffPerKwh}
              onChange={(event) => onChange("baseTariffPerKwh", event.target.value)}
              error={errors.baseTariffPerKwh}
            />
            <Input
              label="Ceza tarifesi (TRY/kWh)"
              type="number"
              min="0"
              step="0.01"
              value={values.penaltyTariffPerKwh}
              onChange={(event) => onChange("penaltyTariffPerKwh", event.target.value)}
              error={errors.penaltyTariffPerKwh}
            />
          </div>
        )}
      </div>
    </div>
  );
}
