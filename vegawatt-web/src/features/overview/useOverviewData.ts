import { useMemo } from "react";
import { useAllHomesLiveStatus } from "../../shared/hooks/useHomesQueries";
import type { ApplianceLiveStatus, HomeLiveStatus } from "../../shared/types/home";
import { toSafeNumber } from "../../shared/utils/format";
import { getHomeHealthStatus } from "../../shared/utils/homeStatus";

const STALE_THRESHOLD_MS = 30_000;
const TOP_CONSUMERS_LIMIT = 5;

export interface TopConsumer {
  appliance: ApplianceLiveStatus;
  homeId: string;
  homeName: string;
  shareOfTotal: number;
}

export interface AttentionItem {
  homeId: string;
  homeName: string;
  reason: string;
}

export interface OverviewData {
  isLoading: boolean;
  isError: boolean;
  homeCount: number;
  liveCount: number;
  staleCount: number;
  totalPowerWatt: number;
  totalCurrentCost: number;
  maxBudgetQuotaPercentage: number;
  projectedMonthEndCost: number | null;
  topConsumers: TopConsumer[];
  attentionItems: AttentionItem[];
  homes: HomeLiveStatus[];
}

function projectMonthEndCost(currentCost: number): number | null {
  const now = new Date();
  const dayOfMonth = now.getDate();
  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  if (dayOfMonth <= 0 || currentCost <= 0) return null;
  return currentCost * (daysInMonth / dayOfMonth);
}

export function useOverviewData(): OverviewData {
  const { homes, isLoading, isError } = useAllHomesLiveStatus();

  return useMemo(() => {
    const now = Date.now();
    let liveCount = 0;
    let staleCount = 0;
    let totalPowerWatt = 0;
    let totalCurrentCost = 0;
    let maxBudgetQuotaPercentage = 0;
    const attentionItems: AttentionItem[] = [];
    const allAppliances: TopConsumer[] = [];

    for (const home of homes) {
      const isStale = now - new Date(home.lastUpdatedAt).getTime() > STALE_THRESHOLD_MS;
      if (isStale) staleCount += 1;
      else liveCount += 1;

      totalCurrentCost += toSafeNumber(home.currentCost);
      maxBudgetQuotaPercentage = Math.max(maxBudgetQuotaPercentage, toSafeNumber(home.budgetQuotaPercentage));

      const status = getHomeHealthStatus(home);
      const anomalousAppliances = home.appliances.filter((appliance) => appliance.anomalous);
      if (status === "CRITICAL" || status === "PENALTY") {
        attentionItems.push({ homeId: home.homeId, homeName: home.homeName, reason: "Kota aşıldı" });
      } else if (status === "WARNING") {
        attentionItems.push({ homeId: home.homeId, homeName: home.homeName, reason: "Bütçe %80 seviyesinde" });
      }
      for (const appliance of anomalousAppliances) {
        attentionItems.push({
          homeId: home.homeId,
          homeName: home.homeName,
          reason: `${appliance.applianceName} anomali`,
        });
      }

      for (const appliance of home.appliances) {
        totalPowerWatt += toSafeNumber(appliance.currentPowerWatt);
        allAppliances.push({ appliance, homeId: home.homeId, homeName: home.homeName, shareOfTotal: 0 });
      }
    }

    const topConsumers = allAppliances
      .sort((a, b) => toSafeNumber(b.appliance.currentPowerWatt) - toSafeNumber(a.appliance.currentPowerWatt))
      .slice(0, TOP_CONSUMERS_LIMIT)
      .map((entry) => ({
        ...entry,
        shareOfTotal: totalPowerWatt > 0 ? toSafeNumber(entry.appliance.currentPowerWatt) / totalPowerWatt : 0,
      }));

    return {
      isLoading,
      isError,
      homeCount: homes.length,
      liveCount,
      staleCount,
      totalPowerWatt,
      totalCurrentCost,
      maxBudgetQuotaPercentage,
      projectedMonthEndCost: projectMonthEndCost(totalCurrentCost),
      topConsumers,
      attentionItems,
      homes,
    };
  }, [homes, isLoading, isError]);
}
