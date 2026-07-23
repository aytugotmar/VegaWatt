import { AlertTriangle, CheckCircle2 } from "lucide-react";
import { useMemo } from "react";
import { BudgetTrajectory } from "../overview/BudgetTrajectory";
import { EnergyFlowVisual } from "../overview/EnergyFlowVisual";
import { LivePowerPulse } from "../overview/LivePowerPulse";
import { TopConsumersPanel } from "../overview/TopConsumersPanel";
import type { TopConsumer } from "../overview/useOverviewData";
import type { HomeLiveStatus } from "../../shared/types/home";
import { ProgressBar } from "../../shared/components/ProgressBar";
import { toSafeNumber } from "../../shared/utils/format";

function projectMonthEndCost(currentCost: number): number | null {
  const now = new Date();
  const dayOfMonth = now.getDate();
  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  if (dayOfMonth <= 0 || currentCost <= 0) return null;
  return currentCost * (daysInMonth / dayOfMonth);
}

export function HomeOverviewSection({ home }: { home: HomeLiveStatus }) {
  const totalPowerWatt = useMemo(
    () => home.appliances.reduce((sum, appliance) => sum + toSafeNumber(appliance.currentPowerWatt), 0),
    [home.appliances],
  );

  const topConsumers = useMemo<TopConsumer[]>(() => {
    return [...home.appliances]
      .sort((a, b) => toSafeNumber(b.currentPowerWatt) - toSafeNumber(a.currentPowerWatt))
      .slice(0, 5)
      .map((appliance) => ({
        appliance,
        homeId: home.homeId,
        homeName: home.homeName,
        shareOfTotal: totalPowerWatt > 0 ? toSafeNumber(appliance.currentPowerWatt) / totalPowerWatt : 0,
      }));
  }, [home.appliances, home.homeId, home.homeName, totalPowerWatt]);

  const alerts = useMemo(() => {
    const reasons: string[] = [];
    if (home.penaltyActive) reasons.push("Ceza tarifesi aktif");
    if (toSafeNumber(home.energyQuotaPercentage) >= 80) reasons.push("Enerji kotası %80 üzerinde");
    if (toSafeNumber(home.budgetQuotaPercentage) >= 80) reasons.push("Bütçe kotası %80 üzerinde");
    for (const appliance of home.appliances) {
      if (appliance.anomalous) reasons.push(`${appliance.applianceName} anomali`);
    }
    return reasons;
  }, [home]);

  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <LivePowerPulse totalPowerWatt={totalPowerWatt} isLoading={false} />
        <BudgetTrajectory
          totalCurrentCost={toSafeNumber(home.currentCost)}
          maxBudgetQuotaPercentage={toSafeNumber(home.budgetQuotaPercentage)}
          projectedMonthEndCost={projectMonthEndCost(toSafeNumber(home.currentCost))}
        />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="flex flex-col justify-center gap-2 rounded-input border border-border bg-surface p-5">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Kota Kullanımı</span>
          <ProgressBar label="Enerji kotası" percentage={home.energyQuotaPercentage} />
          <ProgressBar label="Bütçe kotası" percentage={home.budgetQuotaPercentage} />
        </div>
        <div className="flex flex-col gap-2 rounded-input border border-border bg-surface p-5">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Aktif Uyarılar</span>
          {alerts.length === 0 ? (
            <div className="flex items-center gap-2 py-1 text-sm text-text-secondary">
              <CheckCircle2 className="h-4 w-4 text-success" aria-hidden="true" />
              Her şey normal görünüyor.
            </div>
          ) : (
            <ul className="flex flex-col gap-1.5">
              {alerts.map((reason) => (
                <li key={reason} className="flex items-center gap-2 text-sm text-text-primary">
                  <AlertTriangle className="h-4 w-4 shrink-0 text-warning" aria-hidden="true" />
                  {reason}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <EnergyFlowVisual totalPowerWatt={totalPowerWatt} appliances={home.appliances} />
        <TopConsumersPanel consumers={topConsumers} linkToHomes={false} />
      </div>
    </div>
  );
}
