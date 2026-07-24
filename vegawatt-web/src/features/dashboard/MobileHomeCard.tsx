import type { HomeLiveSummary } from "../../shared/types/home";
import { StatusBadge } from "../../shared/components/StatusBadge";
import { ProgressBar } from "../../shared/components/ProgressBar";
import { formatCurrency, formatRelativeTime } from "../../shared/utils/format";
import { getHomeHealthStatus } from "../../shared/utils/homeStatus";

interface MobileHomeCardProps {
  home: HomeLiveSummary;
  onSelect: (homeId: string) => void;
}

export function MobileHomeCard({ home, onSelect }: MobileHomeCardProps) {
  const status = getHomeHealthStatus(home);

  return (
    <button
      type="button"
      onClick={() => onSelect(home.homeId)}
      className="card-glass flex w-full flex-col gap-3 rounded-card border border-border p-4 text-left shadow-[var(--shadow-card)] transition hover:shadow-[var(--shadow-card-hover)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
      data-testid="mobile-home-card"
    >
      <div className="flex items-center justify-between gap-2">
        <h3 className="truncate font-semibold text-text-primary">{home.homeName}</h3>
        <StatusBadge status={status} />
      </div>
      <p className="tabular-nums text-2xl font-semibold tracking-tight text-text-primary">
        {formatCurrency(home.currentCost)}
      </p>
      <div className="flex flex-col gap-2">
        <ProgressBar label="Enerji Kotası" percentage={home.energyQuotaPercentage} />
        <ProgressBar label="Bütçe Kotası" percentage={home.budgetQuotaPercentage} />
      </div>
      <p className="text-xs text-text-muted">Son veri: {formatRelativeTime(home.lastUpdatedAt)}</p>
    </button>
  );
}
