import type { HomeLiveSummary } from "../types/home";
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

const STATUS_LABELS: Record<HomeHealthStatus, string> = {
  NORMAL: "Normal",
  WARNING: "Kota yaklaşıyor",
  CRITICAL: "Kota aşıldı",
  PENALTY: "Yüksek tarife",
};

export function getHomeStatusLabel(status: HomeHealthStatus): string {
  return STATUS_LABELS[status];
}

export type QuotaTone = "normal" | "warning" | "critical";

export function getQuotaTone(percentage: string | number | null | undefined): QuotaTone {
  const value = toSafeNumber(percentage);
  if (value >= 100) return "critical";
  if (value >= 80) return "warning";
  return "normal";
}
