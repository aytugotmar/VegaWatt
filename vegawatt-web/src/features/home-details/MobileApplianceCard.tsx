import type { ApplianceLiveStatus } from "../../shared/types/home";
import { getApplianceDisplayName, getApplianceIcon } from "../../shared/constants/applianceTypes";
import { ApplianceStatusBadge } from "../../shared/components/ApplianceStatusBadge";
import { formatEnergy, formatPower, formatRelativeTime } from "../../shared/utils/format";
import { ApplianceBreachIndicator } from "./ApplianceBreachIndicator";

interface MobileApplianceCardProps {
  appliance: ApplianceLiveStatus;
}

export function MobileApplianceCard({ appliance }: MobileApplianceCardProps) {
  const Icon = getApplianceIcon(appliance.applianceType, appliance.catalogIconKey);

  return (
    <div
      className={`flex flex-col gap-2 rounded-card border border-border bg-surface p-3 ${
        appliance.anomalous ? "border-l-4 border-l-danger" : ""
      }`}
      data-testid="mobile-appliance-card"
    >
      <div className="flex items-center justify-between gap-2">
        <div className="flex min-w-0 items-center gap-2">
          <Icon className="h-4 w-4 shrink-0 text-text-muted" aria-hidden="true" />
          <div className="flex min-w-0 flex-col">
            <span className="truncate font-medium text-text-primary">{appliance.applianceName}</span>
            <span className="text-xs text-text-muted">
              {getApplianceDisplayName(appliance.applianceType, appliance.catalogDisplayName)}
            </span>
          </div>
        </div>
        <ApplianceStatusBadge appliance={appliance} className="shrink-0" />
      </div>

      <div className="grid grid-cols-2 gap-x-3 gap-y-1 text-xs">
        <span className="text-text-muted">Güncel güç</span>
        <span className="tabular-nums text-right text-text-primary">{formatPower(appliance.currentPowerWatt)}</span>
        <span className="text-text-muted">Güvenli limit</span>
        <span className="tabular-nums text-right text-text-primary">
          {appliance.safePowerLimitWatt === null ? "—" : formatPower(appliance.safePowerLimitWatt)}
        </span>
        <span className="text-text-muted">Toplam enerji</span>
        <span className="tabular-nums text-right text-text-primary">
          {formatEnergy(appliance.accumulatedEnergyKwh)}
        </span>
      </div>

      <div className="flex items-center justify-between border-t border-border pt-2 text-xs text-text-muted">
        <ApplianceBreachIndicator consecutiveBreachCount={appliance.consecutiveBreachCount} />
        <span>{formatRelativeTime(appliance.lastUpdatedAt)}</span>
      </div>
    </div>
  );
}
