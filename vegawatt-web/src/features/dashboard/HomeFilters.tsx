import { Search } from "lucide-react";
import type { HomeHealthStatus } from "../../shared/utils/homeStatus";
import { useLanguage } from "../../shared/i18n/LanguageContext";

export type StatusFilter = "ALL" | HomeHealthStatus;
export type TariffFilter = "ALL" | "BASE" | "PENALTY";
export type SortOption = "CRITICAL" | "COST_DESC" | "ENERGY_DESC" | "RECENT" | "NAME";

interface HomeFiltersProps {
  search: string;
  onSearchChange: (value: string) => void;
  status: StatusFilter;
  onStatusChange: (value: StatusFilter) => void;
  tariff: TariffFilter;
  onTariffChange: (value: TariffFilter) => void;
  sort: SortOption;
  onSortChange: (value: SortOption) => void;
}

export function HomeFilters({
  search,
  onSearchChange,
  status,
  onStatusChange,
  tariff,
  onTariffChange,
  sort,
  onSortChange,
}: HomeFiltersProps) {
  const { t } = useLanguage();

  const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
    { value: "ALL", label: t("filter.all") },
    { value: "NORMAL", label: t("health.normal") },
    { value: "WARNING", label: t("health.approachingQuota") },
    { value: "CRITICAL", label: t("health.quotaExceeded") },
    { value: "PENALTY", label: t("health.penaltyTariff") },
  ];

  const TARIFF_OPTIONS: { value: TariffFilter; label: string }[] = [
    { value: "ALL", label: t("filter.allTariffs") },
    { value: "BASE", label: t("card.standardTariff") },
    { value: "PENALTY", label: t("card.penaltyTariff") },
  ];

  const SORT_OPTIONS: { value: SortOption; label: string }[] = [
    { value: "CRITICAL", label: t("filter.critical") },
    { value: "COST_DESC", label: t("filter.costDesc") },
    { value: "ENERGY_DESC", label: t("filter.energyDesc") },
    { value: "RECENT", label: t("filter.recent") },
    { value: "NAME", label: t("filter.name") },
  ];

  return (
    <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center">
      <div className="relative flex-1 sm:min-w-[220px] sm:max-w-xs">
        <Search
          className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted"
          aria-hidden="true"
        />
        <input
          type="search"
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder={t("filter.searchPlaceholder")}
          aria-label={t("filter.searchPlaceholder")}
          className="form-input pl-8"
        />
      </div>

      <div className="flex w-full sm:w-auto items-center gap-1.5 text-sm">
        <label htmlFor="dashboard-status-filter" className="sr-only">
          Durum
        </label>
        <select
          id="dashboard-status-filter"
          value={status}
          onChange={(event) => onStatusChange(event.target.value as StatusFilter)}
          className="form-input w-full sm:w-auto"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex w-full sm:w-auto items-center gap-1.5 text-sm">
        <label htmlFor="dashboard-tariff-filter" className="sr-only">
          Tarife
        </label>
        <select
          id="dashboard-tariff-filter"
          value={tariff}
          onChange={(event) => onTariffChange(event.target.value as TariffFilter)}
          className="form-input w-full sm:w-auto"
        >
          {TARIFF_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex w-full sm:w-auto items-center gap-1.5 text-sm">
        <label htmlFor="dashboard-sort" className="sr-only">
          Sırala
        </label>
        <select
          id="dashboard-sort"
          value={sort}
          onChange={(event) => onSortChange(event.target.value as SortOption)}
          className="form-input w-full sm:w-auto"
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
