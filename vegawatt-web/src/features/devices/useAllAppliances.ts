import { useMemo } from "react";
import { useAllHomesLiveStatus } from "../../shared/hooks/useHomesQueries";
import type { ApplianceLiveStatus } from "../../shared/types/home";
import { toSafeNumber } from "../../shared/utils/format";

export interface DeviceRow {
  appliance: ApplianceLiveStatus;
  homeId: string;
  homeName: string;
  limitUsagePercentage: number | null;
  shareOfTotalPower: number;
  /** This billing period's energy for the device — real value, not derived. */
  accumulatedEnergyKwh: number;
  /**
   * Proportional estimate only: the home's real accumulated cost split across its appliances by
   * each one's share of the home's total accumulated energy. Not an actual per-device billing
   * figure (the backend doesn't meter cost per appliance) — always present this as an estimate.
   */
  estimatedCost: number | null;
}

export interface AllAppliancesResult {
  isLoading: boolean;
  isError: boolean;
  devices: DeviceRow[];
}

export function useAllAppliances(): AllAppliancesResult {
  const { homes, isLoading, isError } = useAllHomesLiveStatus();

  const devices = useMemo(() => {
    const totalPowerWatt = homes.reduce(
      (sum, home) => sum + home.appliances.reduce((s, a) => s + toSafeNumber(a.currentPowerWatt), 0),
      0,
    );

    const rows: DeviceRow[] = [];
    for (const home of homes) {
      const homeTotalEnergyKwh = home.appliances.reduce((s, a) => s + toSafeNumber(a.accumulatedEnergyKwh), 0);
      const homeCurrentCost = toSafeNumber(home.currentCost);

      for (const appliance of home.appliances) {
        const power = toSafeNumber(appliance.currentPowerWatt);
        const limit = appliance.safePowerLimitWatt !== null ? toSafeNumber(appliance.safePowerLimitWatt) : null;
        const applianceEnergyKwh = toSafeNumber(appliance.accumulatedEnergyKwh);
        rows.push({
          appliance,
          homeId: home.homeId,
          homeName: home.homeName,
          limitUsagePercentage: limit && limit > 0 ? (power / limit) * 100 : null,
          shareOfTotalPower: totalPowerWatt > 0 ? power / totalPowerWatt : 0,
          accumulatedEnergyKwh: applianceEnergyKwh,
          estimatedCost: homeTotalEnergyKwh > 0 ? homeCurrentCost * (applianceEnergyKwh / homeTotalEnergyKwh) : null,
        });
      }
    }
    return rows;
  }, [homes]);

  return { isLoading, isError, devices };
}

export interface UseDeviceByIdResult {
  device: DeviceRow | undefined;
  isLoading: boolean;
  isError: boolean;
}

export function useDeviceById(applianceId: string | undefined): UseDeviceByIdResult {
  const { devices, isLoading, isError } = useAllAppliances();
  const device = useMemo(
    () => devices.find((d) => d.appliance.applianceId === applianceId),
    [devices, applianceId],
  );
  return { device, isLoading, isError };
}
