import { Plus, Trash2 } from "lucide-react";
import type { CustomAppliance, FieldErrors } from "../../home-registration/registrationSchema";

interface CustomApplianceFormProps {
  customAppliances: CustomAppliance[];
  customErrors: Record<number, FieldErrors>;
  onAddCustom: () => void;
  onRemoveCustom: (id: number) => void;
  onCustomChange: (id: number, field: keyof Omit<CustomAppliance, "id">, value: string) => void;
}

/** Extracted verbatim from Aşama 9's `AppliancesStep.tsx` — same fields, same validation, only the
 * container changed. */
export function CustomApplianceForm({
  customAppliances,
  customErrors,
  onAddCustom,
  onRemoveCustom,
  onCustomChange,
}: CustomApplianceFormProps) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-text-primary">Listede olmayan cihaz ekle</h3>
        <button
          type="button"
          onClick={onAddCustom}
          className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-hover"
        >
          <Plus className="h-3.5 w-3.5" aria-hidden="true" />
          Cihaz ekle
        </button>
      </div>
      {customAppliances.length === 0 ? (
        <p className="text-sm text-text-muted">Manuel eklenen cihazlar burada listelenir.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {customAppliances.map((appliance) => (
            <CustomApplianceRow
              key={appliance.id}
              appliance={appliance}
              errors={customErrors[appliance.id] ?? {}}
              onChange={(field, value) => onCustomChange(appliance.id, field, value)}
              onRemove={() => onRemoveCustom(appliance.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function CustomApplianceRow({
  appliance,
  errors,
  onChange,
  onRemove,
}: {
  appliance: CustomAppliance;
  errors: FieldErrors;
  onChange: (field: keyof Omit<CustomAppliance, "id">, value: string) => void;
  onRemove: () => void;
}) {
  return (
    <div className="grid grid-cols-2 gap-2 rounded-input border border-border p-3 sm:grid-cols-6">
      <span className="flex flex-col gap-0.5 text-xs sm:col-span-2">
        <input
          placeholder="Cihaz adı"
          value={appliance.name}
          onChange={(event) => onChange("name", event.target.value)}
          className={`form-input ${errors.name ? "border-danger" : ""}`}
        />
        {errors.name && <span className="text-danger">{errors.name}</span>}
      </span>
      <span className="flex flex-col gap-0.5 text-xs">
        <input
          placeholder="Tip (örn. HEATER)"
          value={appliance.type}
          onChange={(event) => onChange("type", event.target.value)}
          className={`form-input ${errors.type ? "border-danger" : ""}`}
        />
        {errors.type && <span className="text-danger">{errors.type}</span>}
      </span>
      <span className="flex flex-col gap-0.5 text-xs">
        <input
          type="number"
          min="0"
          placeholder="Güvenli limit (W)"
          value={appliance.safePowerLimitWatt}
          onChange={(event) => onChange("safePowerLimitWatt", event.target.value)}
          className={`form-input ${errors.safePowerLimitWatt ? "border-danger" : ""}`}
        />
        {errors.safePowerLimitWatt && <span className="text-danger">{errors.safePowerLimitWatt}</span>}
      </span>
      <span className="flex flex-col gap-0.5 text-xs">
        <input
          type="number"
          min="0"
          placeholder="Min (W)"
          value={appliance.simulationMinWatt}
          onChange={(event) => onChange("simulationMinWatt", event.target.value)}
          className={`form-input ${errors.simulationMinWatt ? "border-danger" : ""}`}
        />
        {errors.simulationMinWatt && <span className="text-danger">{errors.simulationMinWatt}</span>}
      </span>
      <div className="flex items-start gap-2">
        <span className="flex flex-1 flex-col gap-0.5 text-xs">
          <input
            type="number"
            min="0"
            placeholder="Maks (W)"
            value={appliance.simulationMaxWatt}
            onChange={(event) => onChange("simulationMaxWatt", event.target.value)}
            className={`form-input ${errors.simulationMaxWatt ? "border-danger" : ""}`}
          />
          {errors.simulationMaxWatt && <span className="text-danger">{errors.simulationMaxWatt}</span>}
        </span>
        <button
          type="button"
          onClick={onRemove}
          aria-label="Cihazı kaldır"
          className="mt-0.5 shrink-0 rounded-input p-2 text-text-muted hover:bg-danger-soft hover:text-danger"
        >
          <Trash2 className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}
