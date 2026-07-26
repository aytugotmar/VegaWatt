import { memo, type KeyboardEvent } from "react";
import type { HomeLiveSummary } from "../../shared/types/home";
import { StatusBadge } from "../../shared/components/StatusBadge";
import { ProgressBar } from "../../shared/components/ProgressBar";
import { TableRowSkeleton } from "../../shared/components/Skeleton";
import { formatCurrency, formatEnergy, formatRelativeTime } from "../../shared/utils/format";
import { getHomeHealthStatus } from "../../shared/utils/homeStatus";
import { useLanguage } from "../../shared/i18n/LanguageContext";

interface HomeTableProps {
  homes: HomeLiveSummary[];
  onSelect: (homeId: string) => void;
  loadingRowCount?: number;
}

const COLUMN_COUNT = 8;

export function HomeTable({ homes, onSelect, loadingRowCount }: HomeTableProps) {
  const { t } = useLanguage();

  return (
    <div className="hidden overflow-x-auto rounded-card border border-border bg-surface shadow-[var(--shadow-card)] sm:block">
      <table className="w-full min-w-[840px] border-collapse text-sm">
        <thead>
          <tr className="border-b border-border-strong text-left text-xs font-semibold uppercase tracking-wide text-text-secondary">
            <th className="px-4 py-3">{t("table.home")}</th>
            <th className="px-4 py-3">{t("table.status")}</th>
            <th className="px-4 py-3">{t("table.totalEnergy")}</th>
            <th className="px-4 py-3">{t("card.energyQuota")}</th>
            <th className="px-4 py-3">{t("card.budgetQuota")}</th>
            <th className="px-4 py-3">{t("table.currentCost")}</th>
            <th className="px-4 py-3">{t("table.tariff")}</th>
            <th className="px-4 py-3">{t("table.lastData")}</th>
          </tr>
        </thead>
        <tbody>
          {loadingRowCount
            ? Array.from({ length: loadingRowCount }).map((_, index) => (
                <TableRowSkeleton key={index} columns={COLUMN_COUNT} />
              ))
            : homes.map((home) => <HomeTableRow key={home.homeId} home={home} onSelect={onSelect} />)}
        </tbody>
      </table>
    </div>
  );
}

const HomeTableRow = memo(function HomeTableRow({
  home,
  onSelect,
}: {
  home: HomeLiveSummary;
  onSelect: (homeId: string) => void;
}) {
  const { t, language } = useLanguage();
  const status = getHomeHealthStatus(home);

  function handleKeyDown(event: KeyboardEvent<HTMLTableRowElement>) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelect(home.homeId);
    }
  }

  return (
    <tr
      role="button"
      tabIndex={0}
      aria-label={`${home.homeName} ${t("status.details")}`}
      onClick={() => onSelect(home.homeId)}
      onKeyDown={handleKeyDown}
      className="cursor-pointer border-b border-border last:border-b-0 transition hover:bg-surface-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary"
    >
      <td className="px-4 py-3 font-medium text-text-primary">{home.homeName}</td>
      <td className="px-4 py-3">
        <StatusBadge status={status} />
      </td>
      <td className="tabular-nums px-4 py-3 text-text-secondary">{formatEnergy(home.currentEnergyKwh, language)}</td>
      <td className="px-4 py-3">
        <div className="w-32">
          <ProgressBar label={t("card.energyQuota")} percentage={home.energyQuotaPercentage} />
        </div>
      </td>
      <td className="px-4 py-3">
        <div className="w-32">
          <ProgressBar label={t("card.budgetQuota")} percentage={home.budgetQuotaPercentage} />
        </div>
      </td>
      <td className="tabular-nums px-4 py-3 font-medium text-text-primary">{formatCurrency(home.currentCost, language)}</td>
      <td className="px-4 py-3 text-text-secondary">
        {home.tariffState === "PENALTY" ? t("card.penaltyTariff") : t("card.standardTariff")}
      </td>
      <td className="px-4 py-3 text-text-muted">{formatRelativeTime(home.lastUpdatedAt, language)}</td>
    </tr>
  );
});
