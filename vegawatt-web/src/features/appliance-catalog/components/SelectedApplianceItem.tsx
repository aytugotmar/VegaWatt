import { ChevronDown, Minus, Plus } from "lucide-react";
import type { ChangeEvent } from "react";
import { getApplianceCatalogIcon } from "../../../shared/constants/applianceCatalogIcons";
import type { FieldErrors } from "../../home-registration/registrationSchema";
import type { SelectedCatalogAppliance, WattField } from "../hooks/useCatalogSelection";
import { getBehaviorProfileLabel } from "../model/applianceCatalogLabels";

interface SelectedApplianceItemProps {
  catalogDisplayName: string;
  catalogIconKey: string;
  instances: SelectedCatalogAppliance[];
  expanded: boolean;
  onToggleExpanded: () => void;
  errors: Record<string, FieldErrors>;
  onIncrement: () => void;
  onDecrement: () => void;
  onRename: (instanceKey: string, name: string) => void;
  onUpdateOverride: (instanceKey: string, field: WattField, value: string) => void;
}

/** One "Klima  1 [-][+]" row grouping every instance of a single catalog item — the tray never
 * mutates the catalog item itself; each instance underneath carries its own independent form
 * state (yapılacak.md §20.6). */
export function SelectedApplianceItem({
  catalogDisplayName,
  catalogIconKey,
  instances,
  expanded,
  onToggleExpanded,
  errors,
  onIncrement,
  onDecrement,
  onRename,
  onUpdateOverride,
}: SelectedApplianceItemProps) {
  const Icon = getApplianceCatalogIcon(catalogIconKey);

  return (
    <div className="rounded-input border border-border">
      <div className="flex items-center gap-2 px-3 py-2">
        <Icon className="h-4 w-4 shrink-0 text-text-secondary" aria-hidden="true" />
        <button
          type="button"
          onClick={onToggleExpanded}
          aria-expanded={expanded}
          className="flex flex-1 items-center gap-1 text-left text-sm font-medium text-text-primary"
        >
          {catalogDisplayName}
          <ChevronDown className={`h-3 w-3 transition ${expanded ? "rotate-180" : ""}`} aria-hidden="true" />
        </button>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={onDecrement}
            aria-label={`${catalogDisplayName} adedini azalt`}
            className="flex h-6 w-6 items-center justify-center rounded-full border border-border text-text-secondary hover:bg-surface-subtle"
          >
            <Minus className="h-3 w-3" aria-hidden="true" />
          </button>
          <span className="tabular-nums w-5 text-center text-sm font-semibold text-text-primary">
            {instances.length}
          </span>
          <button
            type="button"
            onClick={onIncrement}
            aria-label={`${catalogDisplayName} adedini artır`}
            className="flex h-6 w-6 items-center justify-center rounded-full border border-border text-text-secondary hover:bg-surface-subtle"
          >
            <Plus className="h-3 w-3" aria-hidden="true" />
          </button>
        </div>
      </div>

      {expanded && (
        <div className="flex flex-col gap-2 border-t border-border px-3 py-2">
          {instances.map((instance) => (
            <SelectedApplianceInstanceRow
              key={instance.instanceKey}
              instance={instance}
              errors={errors[instance.instanceKey] ?? {}}
              onRename={(name) => onRename(instance.instanceKey, name)}
              onUpdateOverride={(field, value) => onUpdateOverride(instance.instanceKey, field, value)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function SelectedApplianceInstanceRow({
  instance,
  errors,
  onRename,
  onUpdateOverride,
}: {
  instance: SelectedCatalogAppliance;
  errors: FieldErrors;
  onRename: (name: string) => void;
  onUpdateOverride: (field: WattField, value: string) => void;
}) {
  const hasStandby = instance.defaultStandbyMinWatt !== null && instance.defaultStandbyMaxWatt !== null;

  return (
    <details className="rounded-input bg-surface-subtle p-2 text-xs">
      <summary className="flex cursor-pointer items-center justify-between gap-2">
        <span className="flex-1 truncate font-medium text-text-primary">{instance.name}</span>
        <span className="text-[10px] text-text-muted">Gelişmiş ayarlar</span>
      </summary>
      <div className="mt-2 flex flex-col gap-2">
        <label className="flex flex-col gap-0.5 text-[11px] text-text-secondary">
          Cihaz adı
          <input
            value={instance.name}
            onChange={(event: ChangeEvent<HTMLInputElement>) => onRename(event.target.value)}
            className={`form-input px-1.5 py-1 text-xs ${errors.name ? "border-danger" : ""}`}
          />
          {errors.name && <span className="text-danger">{errors.name}</span>}
        </label>
        <div className="text-[11px] text-text-secondary">
          Kullanım profili: <span className="font-medium text-text-primary">{getBehaviorProfileLabel(instance.catalogBehaviorProfile)}</span>
        </div>
        <div className="grid grid-cols-3 gap-1.5">
          <LimitField
            label="Güvenli limit (W)"
            value={instance.safePowerLimitWatt}
            error={errors.safePowerLimitWatt}
            onChange={(value) => onUpdateOverride("safePowerLimitWatt", value)}
          />
          <LimitField
            label="Aktif min (W)"
            value={instance.simulationMinWatt}
            error={errors.simulationMinWatt}
            onChange={(value) => onUpdateOverride("simulationMinWatt", value)}
          />
          <LimitField
            label="Aktif maks (W)"
            value={instance.simulationMaxWatt}
            error={errors.simulationMaxWatt}
            onChange={(value) => onUpdateOverride("simulationMaxWatt", value)}
          />
        </div>
        {hasStandby && (
          <p className="text-[11px] text-text-muted">
            Bekleme aralığı (salt okunur): {instance.defaultStandbyMinWatt}–{instance.defaultStandbyMaxWatt} W
          </p>
        )}
      </div>
    </details>
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
    <label className="flex flex-col gap-0.5 text-[11px] text-text-secondary">
      {label}
      <input
        type="number"
        min="0"
        value={value}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onChange(event.target.value)}
        className={`form-input px-1.5 py-1 text-xs ${error ? "border-danger" : ""}`}
      />
      {error && <span className="text-danger">{error}</span>}
    </label>
  );
}
