import type { ApplianceLiveStatus } from "../../shared/types/home";
import { getApplianceDisplayName, getApplianceIcon } from "../../shared/constants/applianceTypes";
import { ApplianceStatusBadge } from "../../shared/components/ApplianceStatusBadge";
import { formatEnergy, formatPower, formatRelativeTime } from "../../shared/utils/format";
import { ApplianceBreachIndicator } from "./ApplianceBreachIndicator";
import { MobileApplianceCard } from "./MobileApplianceCard";

interface AppliancesTableProps {
  appliances: ApplianceLiveStatus[];
}

export function AppliancesTable({ appliances }: AppliancesTableProps) {
  return (
    <>
      <div className="flex flex-col gap-2 sm:hidden">
        {appliances.map((appliance) => (
          <MobileApplianceCard key={appliance.applianceId} appliance={appliance} />
        ))}
      </div>
      <div className="hidden overflow-x-auto max-w-full rounded-card border border-border bg-surface sm:block">
      <table className="w-full min-w-[720px] border-collapse text-sm">
        <thead>
          <tr className="border-b border-border-strong bg-surface-subtle text-left text-xs font-semibold uppercase tracking-wide text-text-secondary">
            <th className="px-3 py-2.5">Cihaz</th>
            <th className="px-3 py-2.5">Durum</th>
            <th className="px-3 py-2.5">Güncel güç</th>
            <th className="px-3 py-2.5">Güvenli limit</th>
            <th className="px-3 py-2.5">Ardışık ihlal</th>
            <th className="px-3 py-2.5">Toplam enerji</th>
            <th className="px-3 py-2.5">Son veri</th>
          </tr>
        </thead>
        <tbody>
          {appliances.map((appliance) => {
            const Icon = getApplianceIcon(appliance.applianceType, appliance.catalogIconKey);
            return (
              <tr
                key={appliance.applianceId}
                data-testid="appliance-row"
                className={`border-b border-border bg-surface last:border-b-0 ${
                  appliance.anomalous ? "border-l-4 border-l-danger" : ""
                }`}
              >
                <td className="px-3 py-2.5">
                  <div className="flex items-center gap-2">
                    <Icon className="h-4 w-4 shrink-0 text-text-muted" aria-hidden="true" />
                    <div className="flex min-w-0 flex-col">
                      <span className="truncate font-medium text-text-primary">{appliance.applianceName}</span>
                      <span className="text-xs text-text-muted">
                        {getApplianceDisplayName(appliance.applianceType, appliance.catalogDisplayName)}
                      </span>
                    </div>
                  </div>
                </td>
                <td className="px-3 py-2.5">
                  <ApplianceStatusBadge appliance={appliance} />
                </td>
                <td className="tabular-nums px-3 py-2.5 text-text-secondary">
                  {formatPower(appliance.currentPowerWatt)}
                </td>
                <td className="tabular-nums px-3 py-2.5 text-text-secondary">
                  {appliance.safePowerLimitWatt === null ? "—" : formatPower(appliance.safePowerLimitWatt)}
                </td>
                <td className="px-3 py-2.5">
                  <ApplianceBreachIndicator consecutiveBreachCount={appliance.consecutiveBreachCount} />
                </td>
                <td className="tabular-nums px-3 py-2.5 text-text-secondary">
                  {formatEnergy(appliance.accumulatedEnergyKwh)}
                </td>
                <td className="px-3 py-2.5 text-text-muted">{formatRelativeTime(appliance.lastUpdatedAt)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      </div>
    </>
  );
}
