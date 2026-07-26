import { ArrowRight, Home as HomeIcon } from "lucide-react";
import type { HomeLiveSummary } from "../../shared/types/home";
import { StatusBadge } from "../../shared/components/StatusBadge";
import { ProgressBar } from "../../shared/components/ProgressBar";
import { formatCurrency, formatEnergy, formatRelativeTime } from "../../shared/utils/format";
import { getHomeHealthStatus } from "../../shared/utils/homeStatus";
import { useLanguage } from "../../shared/i18n/LanguageContext";

interface HomeCardProps {
  home: HomeLiveSummary;
  onSelect: (homeId: string) => void;
}

export function HomeCard({ home, onSelect }: HomeCardProps) {
  const { t, language } = useLanguage();
  const status = getHomeHealthStatus(home);
  const isPenalty = home.tariffState === "PENALTY";

  return (
    <button
      type="button"
      onClick={() => onSelect(home.homeId)}
      aria-label={`${home.homeName} ${t("status.details")}`}
      className="glow-card flex flex-col gap-4 rounded-card border border-border bg-surface p-5 text-left shadow-[var(--shadow-card)] transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-[var(--shadow-card-hover)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
      data-testid="home-card"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-soft text-primary">
            <HomeIcon className="h-5 w-5" aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <h3 className="truncate font-semibold text-text-primary">{home.homeName}</h3>
            <p className="text-xs text-text-muted">{t("card.lastData")} {formatRelativeTime(home.lastUpdatedAt, language)}</p>
          </div>
        </div>
        <span
          className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${
            isPenalty ? "bg-danger-soft text-danger" : "bg-surface-subtle text-text-secondary"
          }`}
        >
          {isPenalty ? t("card.penaltyTariff") : t("card.standardTariff")}
        </span>
      </div>

      <div>
        <span className="text-xs font-medium uppercase tracking-wide text-text-muted">{t("card.currentCost")}</span>
        <p className="glow-text-primary mt-1 text-3xl font-bold tracking-tight tabular-nums text-text-primary">
          {formatCurrency(home.currentCost, language)}
        </p>
      </div>

      <div className="flex items-center justify-between text-xs text-text-secondary">
        <span>{t("card.totalEnergy")}</span>
        <span className="tabular-nums font-medium text-text-primary">{formatEnergy(home.currentEnergyKwh, language)}</span>
      </div>

      <div className="flex flex-col gap-2.5">
        <ProgressBar label={t("card.energyQuota")} percentage={home.energyQuotaPercentage} />
        <ProgressBar label={t("card.budgetQuota")} percentage={home.budgetQuotaPercentage} />
      </div>

      <div className="flex items-center justify-between border-t border-border pt-3">
        <StatusBadge status={status} />
        <span className="group/details flex items-center gap-1 rounded-full px-2.5 py-1.5 text-xs font-semibold text-primary transition hover:bg-primary-soft">
          {t("status.details")}
          <ArrowRight className="h-3.5 w-3.5 transition group-hover/details:translate-x-0.5" aria-hidden="true" />
        </span>
      </div>
    </button>
  );
}
