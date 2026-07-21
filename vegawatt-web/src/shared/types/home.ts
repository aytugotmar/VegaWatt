export type TariffState = "BASE" | "PENALTY";

/**
 * Backend BigDecimal fields serialize as JSON numbers via Jackson, not strings — but large or
 * high-precision values may still arrive as strings depending on the serializer path, so every
 * decimal-shaped field is typed to accept either. Always read these through `toSafeNumber`.
 */
export type DecimalValue = string | number;

export interface HomeLiveSummary {
  homeId: string;
  homeName: string;
  currentEnergyKwh: DecimalValue;
  currentCost: DecimalValue;
  energyQuotaPercentage: DecimalValue;
  budgetQuotaPercentage: DecimalValue;
  tariffState: TariffState;
  penaltyActive: boolean;
  lastUpdatedAt: string;
}

export interface ApplianceLiveStatus {
  applianceId: string;
  applianceName: string;
  applianceType: string | null;
  safePowerLimitWatt: DecimalValue | null;
  currentPowerWatt: DecimalValue;
  accumulatedEnergyKwh: DecimalValue;
  consecutiveBreachCount: number;
  anomalous: boolean;
  lastUpdatedAt: string;
}

export interface HomeLiveStatus extends HomeLiveSummary {
  appliances: ApplianceLiveStatus[];
}

export interface ConsumptionHistoryPoint {
  snapshotTime: string;
  energyKwh: DecimalValue;
  cost: DecimalValue;
  tariffState: TariffState;
}

export type EmailStatus = "PENDING" | "SENT" | "FAILED";

export interface Recommendation {
  id: string;
  triggerType: string;
  content: string;
  fallbackUsed: boolean;
  emailStatus: EmailStatus;
  createdAt: string;
}

export interface ApplianceRegistration {
  name: string;
  type: string;
  safePowerLimitWatt: number;
  simulationMinWatt: number;
  simulationMaxWatt: number;
}

export interface RegisterHomeRequest {
  name: string;
  contactEmail: string;
  energyQuotaKwh: number;
  budgetQuotaTry: number;
  baseTariffPerKwh: number;
  penaltyTariffPerKwh: number;
  appliances: ApplianceRegistration[];
}

export interface RegisterHomeResponse {
  homeId: string;
  name: string;
  contactEmail: string;
}
