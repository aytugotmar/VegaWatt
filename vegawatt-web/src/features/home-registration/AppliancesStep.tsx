import { ChevronDown, Minus, Plus, Trash2 } from "lucide-react";
import type { ChangeEvent } from "react";
import { APPLIANCE_TYPE_PRESETS, type ApplianceTypePreset } from "../../shared/constants/applianceTypes";
import type { CustomAppliance, FieldErrors, PresetSelection } from "./registrationSchema";

interface AppliancesStepProps {
  selections: Record<string, PresetSelection>;
  onTogglePreset: (key: string) => void;
  onQuantityChange: (key: string, delta: number) => void;
  onToggleCustomize: (key: string) => void;
  onLimitChange: (
    key: string,
    field: "safePowerLimitWatt" | "simulationMinWatt" | "simulationMaxWatt",
    value: string,
  ) => void;
  presetErrors: Record<string, FieldErrors>;
  customAppliances: CustomAppliance[];
  customErrors: Record<number, FieldErrors>;
  onAddCustom: () => void;
  onRemoveCustom: (id: number) => void;
  onCustomChange: (id: number, field: keyof Omit<CustomAppliance, "id">, value: string) => void;
  hasAtLeastOneAppliance: boolean;
}

export function AppliancesStep({
  selections,
  onTogglePreset,
  onQuantityChange,
  onToggleCustomize,
  onLimitChange,
  presetErrors,
  customAppliances,
  customErrors,
  onAddCustom,
  onRemoveCustom,
  onCustomChange,
  hasAtLeastOneAppliance,
}: AppliancesStepProps) {
  return (
    <div className="flex flex-col gap-5">
      <div>
        <h3 className="mb-1 text-sm font-semibold text-text-primary">Cihazlar</h3>
        <p className="mb-3 text-xs text-text-secondary">Birden fazla adet seçebilirsiniz, örn. 3 buzdolabı.</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {APPLIANCE_TYPE_PRESETS.map((preset) => (
            <PresetCard
              key={preset.key}
              preset={preset}
              selection={selections[preset.key]}
              errors={presetErrors[preset.key] ?? {}}
              onToggle={() => onTogglePreset(preset.key)}
              onQuantityChange={(delta) => onQuantityChange(preset.key, delta)}
              onToggleCustomize={() => onToggleCustomize(preset.key)}
              onLimitChange={(field, value) => onLimitChange(preset.key, field, value)}
            />
          ))}
        </div>
        {!hasAtLeastOneAppliance && (
          <p className="mt-2 text-xs font-medium text-danger">En az bir cihaz seçmeli veya eklemelisiniz.</p>
        )}
      </div>

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
    </div>
  );
}

interface PresetCardProps {
  preset: ApplianceTypePreset;
  selection: PresetSelection;
  errors: FieldErrors;
  onToggle: () => void;
  onQuantityChange: (delta: number) => void;
  onToggleCustomize: () => void;
  onLimitChange: (field: "safePowerLimitWatt" | "simulationMinWatt" | "simulationMaxWatt", value: string) => void;
}

function PresetCard({ preset, selection, errors, onToggle, onQuantityChange, onToggleCustomize, onLimitChange }: PresetCardProps) {
  const Icon = preset.icon;

  return (
    <div
      className={`flex flex-col gap-2.5 rounded-card border p-3 transition ${
        selection.checked ? "border-primary ring-2 ring-primary/20" : "border-border"
      } bg-surface`}
    >
      <label className="flex cursor-pointer items-center gap-3">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-input bg-surface-subtle text-text-secondary">
          <Icon className="h-5 w-5" aria-hidden="true" />
        </span>
        <span className="flex-1 text-sm font-semibold text-text-primary">{preset.label}</span>
        <input
          type="checkbox"
          checked={selection.checked}
          onChange={onToggle}
          className="h-5 w-5 shrink-0 cursor-pointer rounded border-border accent-primary"
        />
      </label>

      {selection.checked && (
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <span className="text-xs text-text-secondary">Adet</span>
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => onQuantityChange(-1)}
                className="flex h-6 w-6 items-center justify-center rounded-full border border-border text-text-secondary hover:bg-surface-subtle"
                aria-label="Azalt"
              >
                <Minus className="h-3 w-3" aria-hidden="true" />
              </button>
              <span
                data-testid={`preset-quantity-${preset.key}`}
                className="tabular-nums w-5 text-center text-sm font-semibold text-text-primary"
              >
                {selection.quantity}
              </span>
              <button
                type="button"
                onClick={() => onQuantityChange(1)}
                className="flex h-6 w-6 items-center justify-center rounded-full border border-border text-text-secondary hover:bg-surface-subtle"
                aria-label="Artır"
              >
                <Plus className="h-3 w-3" aria-hidden="true" />
              </button>
            </div>
            <button
              type="button"
              onClick={onToggleCustomize}
              className="ml-auto inline-flex items-center gap-1 text-xs font-medium text-text-secondary hover:text-primary"
              aria-expanded={selection.customized}
            >
              Ayarları düzenle
              <ChevronDown className={`h-3 w-3 transition ${selection.customized ? "rotate-180" : ""}`} aria-hidden="true" />
            </button>
          </div>

          {selection.customized && (
            <div className="flex flex-col gap-2 border-t border-border pt-2">
              <p className="text-[11px] text-text-muted">
                Güvenli limit aşıldığında ihlal sayılır; simülasyon aralığı test verisi üretir.
              </p>
              <div className="grid grid-cols-3 gap-1.5">
                <LimitField
                  label="Limit (W)"
                  value={selection.safePowerLimitWatt}
                  error={errors.safePowerLimitWatt}
                  onChange={(value) => onLimitChange("safePowerLimitWatt", value)}
                />
                <LimitField
                  label="Min (W)"
                  value={selection.simulationMinWatt}
                  error={errors.simulationMinWatt}
                  onChange={(value) => onLimitChange("simulationMinWatt", value)}
                />
                <LimitField
                  label="Maks (W)"
                  value={selection.simulationMaxWatt}
                  error={errors.simulationMaxWatt}
                  onChange={(value) => onLimitChange("simulationMaxWatt", value)}
                />
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function LimitField({
  label,
  value,
  error,
  onChange,
}: {
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  return (
    <span className="flex flex-col gap-0.5 text-[11px] text-text-secondary">
      {label}
      <input
        type="number"
        min="0"
        value={value}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onChange(event.target.value)}
        className={`form-input px-1.5 py-1 text-xs ${error ? "border-danger" : ""}`}
      />
      {error && <span className="text-danger">{error}</span>}
    </span>
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
