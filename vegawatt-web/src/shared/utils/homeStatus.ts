import type { ApplianceLiveStatus, HomeLiveSummary } from "../types/home";
import { toSafeNumber } from "./format";

export type HomeHealthStatus = "NORMAL" | "WARNING" | "CRITICAL" | "PENALTY";

type HomeStatusInput = Pick<
  HomeLiveSummary,
  "penaltyActive" | "tariffState" | "energyQuotaPercentage" | "budgetQuotaPercentage"
>;

export function getHomeHealthStatus(home: HomeStatusInput): HomeHealthStatus {
  if (home.penaltyActive || home.tariffState === "PENALTY") return "PENALTY";
  const maxQuotaPercentage = Math.max(
    toSafeNumber(home.energyQuotaPercentage),
    toSafeNumber(home.budgetQuotaPercentage),
  );
  if (maxQuotaPercentage >= 100) return "CRITICAL";
  if (maxQuotaPercentage >= 80) return "WARNING";
  return "NORMAL";
}

const STATUS_KEYS: Record<HomeHealthStatus, string> = {
  NORMAL: "status.normal",
  WARNING: "status.approachingQuota",
  CRITICAL: "status.quotaExceeded",
  PENALTY: "status.highTariff",
};

export function getHomeStatusLabel(status: HomeHealthStatus, t?: (key: string) => string): string {
  if (t) return t(STATUS_KEYS[status]);
  const fallbacks: Record<HomeHealthStatus, string> = {
    NORMAL: "Normal",
    WARNING: "Kota yaklaşıyor",
    CRITICAL: "Kota aşıldı",
    PENALTY: "Yüksek tarife",
  };
  return fallbacks[status];
}

export type QuotaTone = "normal" | "warning" | "critical";

export function getQuotaTone(percentage: string | number | null | undefined): QuotaTone {
  const value = toSafeNumber(percentage);
  if (value >= 100) return "critical";
  if (value >= 80) return "warning";
  return "normal";
}

export type ApplianceHealthTone = "offline" | "stale" | "anomalous" | "warning" | "normal";

type ApplianceStatusInput = Pick<
  ApplianceLiveStatus,
  "telemetryHealthStatus" | "anomalous" | "standbyAnomalyActive"
>;

export function getApplianceHealthTone(appliance: ApplianceStatusInput): ApplianceHealthTone {
  if (appliance.telemetryHealthStatus === "OFFLINE") return "offline";
  if (appliance.telemetryHealthStatus === "STALE") return "stale";
  if (appliance.anomalous) return "anomalous";
  if (appliance.standbyAnomalyActive) return "warning";
  return "normal";
}

const APPLIANCE_TONE_KEYS: Record<ApplianceHealthTone, string> = {
  offline: "status.offline",
  stale: "status.stale",
  anomalous: "status.anomaly",
  warning: "status.standbyWarning",
  normal: "status.normal",
};

export function getApplianceHealthLabel(tone: ApplianceHealthTone, t?: (key: string) => string): string {
  if (t) return t(APPLIANCE_TONE_KEYS[tone]);
  const fallbacks: Record<ApplianceHealthTone, string> = {
    offline: "Çevrimdışı",
    stale: "Veri bekleniyor",
    anomalous: "Anomali",
    warning: "Bekleme uyarısı",
    normal: "Normal",
  };
  return fallbacks[tone];
}
