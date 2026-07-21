import { Search } from "lucide-react";
import type { HomeHealthStatus } from "../../shared/utils/homeStatus";

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

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "Tümü" },
  { value: "NORMAL", label: "Normal" },
  { value: "WARNING", label: "Kota yaklaşıyor" },
  { value: "CRITICAL", label: "Kritik" },
  { value: "PENALTY", label: "Yüksek tarife" },
];

const TARIFF_OPTIONS: { value: TariffFilter; label: string }[] = [
  { value: "ALL", label: "Tüm tarifeler" },
  { value: "BASE", label: "Normal tarife" },
  { value: "PENALTY", label: "Ceza tarifesi" },
];

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: "CRITICAL", label: "En kritik" },
  { value: "COST_DESC", label: "En yüksek maliyet" },
  { value: "ENERGY_DESC", label: "En yüksek enerji" },
  { value: "RECENT", label: "En son güncellenen" },
  { value: "NAME", label: "Ev adına göre" },
];

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
          placeholder="Ev ara..."
          aria-label="Ev ara"
          className="form-input pl-8"
        />
      </div>

      <div className="flex items-center gap-1.5 text-sm">
        <label htmlFor="dashboard-status-filter" className="sr-only">
          Durum
        </label>
        <select
          id="dashboard-status-filter"
          value={status}
          onChange={(event) => onStatusChange(event.target.value as StatusFilter)}
          className="form-input"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex items-center gap-1.5 text-sm">
        <label htmlFor="dashboard-tariff-filter" className="sr-only">
          Tarife
        </label>
        <select
          id="dashboard-tariff-filter"
          value={tariff}
          onChange={(event) => onTariffChange(event.target.value as TariffFilter)}
          className="form-input"
        >
          {TARIFF_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex items-center gap-1.5 text-sm">
        <label htmlFor="dashboard-sort" className="sr-only">
          Sırala
        </label>
        <select
          id="dashboard-sort"
          value={sort}
          onChange={(event) => onSortChange(event.target.value as SortOption)}
          className="form-input"
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
