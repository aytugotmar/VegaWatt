import { Zap } from "lucide-react";
import { useMemo } from "react";
import type { ApplianceLiveStatus } from "../../shared/types/home";
import { getApplianceIcon } from "../../shared/constants/applianceTypes";
import { formatPower, toSafeNumber } from "../../shared/utils/format";

const MAX_VISIBLE_BRANCHES = 4;

interface FlowBranch {
  key: string;
  label: string;
  powerWatt: number;
  icon: ReturnType<typeof getApplianceIcon>;
}

function buildBranches(appliances: ApplianceLiveStatus[]): FlowBranch[] {
  const sorted = [...appliances].sort(
    (a, b) => toSafeNumber(b.currentPowerWatt) - toSafeNumber(a.currentPowerWatt),
  );
  const visible = sorted.slice(0, MAX_VISIBLE_BRANCHES);
  const rest = sorted.slice(MAX_VISIBLE_BRANCHES);

  const branches: FlowBranch[] = visible.map((appliance) => ({
    key: appliance.applianceId,
    label: appliance.applianceName,
    powerWatt: toSafeNumber(appliance.currentPowerWatt),
    icon: getApplianceIcon(appliance.applianceType, appliance.catalogIconKey),
  }));

  if (rest.length > 0) {
    const restPowerWatt = rest.reduce((sum, appliance) => sum + toSafeNumber(appliance.currentPowerWatt), 0);
    branches.push({
      key: "__rest",
      label: `Diğer (${rest.length})`,
      powerWatt: restPowerWatt,
      icon: getApplianceIcon(null),
    });
  }

  return branches;
}

/**
 * A minimal GRID → HOME → appliance flow diagram — the "signature visual" the product review
 * calls for, built entirely from data the overview already fetches (no new backend endpoint).
 * Deliberately not a 3D/animated illustration: a plain connector-line tree, since the review
 * explicitly warns against heavy illustrations diluting the data-forward, corporate tone.
 */
export function EnergyFlowVisual({
  totalPowerWatt,
  appliances,
}: {
  totalPowerWatt: number;
  appliances: ApplianceLiveStatus[];
}) {
  const branches = useMemo(() => buildBranches(appliances), [appliances]);
  const isFlowing = totalPowerWatt > 0;

  return (
    <div className="card-glass flex flex-col gap-3 rounded-input border border-border p-5">
      <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Enerji Akışı</span>

      <div className="flex flex-col items-center">
        <FlowNode label="ŞEBEKE" />
        <FlowConnector active={isFlowing} />
        <FlowNode label="EV" value={formatPower(totalPowerWatt)} emphasized />
      </div>

      {branches.length === 0 ? (
        <p className="py-2 text-center text-sm text-text-muted">Henüz cihaz verisi yok.</p>
      ) : (
        <ul className="flex flex-col">
          {branches.map((branch, index) => {
            const Icon = branch.icon;
            const isLast = index === branches.length - 1;
            return (
              <li key={branch.key} className="flex items-center gap-2">
                <span
                  className={`h-6 w-4 shrink-0 border-l-2 border-border-strong ${isLast ? "rounded-bl-md" : ""}`}
                  style={{ borderBottomWidth: isLast ? 2 : 0 }}
                  aria-hidden="true"
                />
                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-input bg-surface-subtle text-text-secondary">
                  <Icon className="h-3.5 w-3.5" aria-hidden="true" />
                </span>
                <span className="min-w-0 flex-1 truncate text-sm text-text-primary">{branch.label}</span>
                <span className="tabular-nums shrink-0 text-sm font-medium text-text-secondary">
                  {formatPower(branch.powerWatt)}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function FlowNode({ label, value, emphasized = false }: { label: string; value?: string; emphasized?: boolean }) {
  return (
    <div
      className={`flex items-center gap-2 rounded-full border px-3 py-1.5 ${
        emphasized ? "border-energy bg-primary-soft" : "border-border bg-surface-subtle"
      }`}
    >
      <Zap className={`h-3.5 w-3.5 ${emphasized ? "text-energy" : "text-text-muted"}`} aria-hidden="true" />
      <span className={`text-xs font-semibold tracking-wide ${emphasized ? "text-text-primary" : "text-text-muted"}`}>
        {label}
      </span>
      {value && <span className="tabular-nums text-xs font-semibold text-energy">{value}</span>}
    </div>
  );
}

function FlowConnector({ active }: { active: boolean }) {
  return (
    <span
      className={`my-1 h-5 w-px ${active ? "bg-energy animate-flow-pulse" : "bg-border"}`}
      aria-hidden="true"
    />
  );
}
