import type { HomeLiveSummary } from "../../shared/types/home";
import { getHomeHealthStatus, type HomeHealthStatus } from "../../shared/utils/homeStatus";
import { useLanguage } from "../../shared/i18n/LanguageContext";

const STATUS_ORDER: HomeHealthStatus[] = ["NORMAL", "WARNING", "CRITICAL", "PENALTY"];

const DOT_CLASS: Record<HomeHealthStatus, string> = {
  NORMAL: "bg-success",
  WARNING: "bg-warning",
  CRITICAL: "bg-danger",
  PENALTY: "bg-danger",
};

const STATUS_KEY: Record<HomeHealthStatus, string> = {
  NORMAL: "health.normal",
  WARNING: "health.approachingQuota",
  CRITICAL: "health.quotaExceeded",
  PENALTY: "health.penaltyTariff",
};

export function PortfolioHealthStrip({ homes }: { homes: HomeLiveSummary[] }) {
  const { t } = useLanguage();
  const counts: Record<HomeHealthStatus, number> = { NORMAL: 0, WARNING: 0, CRITICAL: 0, PENALTY: 0 };
  for (const home of homes) {
    counts[getHomeHealthStatus(home)] += 1;
  }

  return (
    <div
      className="mb-4 flex flex-wrap items-center gap-x-4 gap-y-1.5 rounded-input border border-border bg-surface px-3.5 py-2 text-xs text-text-secondary"
      role="status"
    >
      {STATUS_ORDER.map((status) => (
        <span key={status} className="inline-flex items-center gap-1.5">
          <span className={`h-2 w-2 shrink-0 rounded-full ${DOT_CLASS[status]}`} aria-hidden="true" />
          <span className="tabular-nums font-semibold text-text-primary">{counts[status]}</span>
          {t(STATUS_KEY[status])}
        </span>
      ))}
    </div>
  );
}
