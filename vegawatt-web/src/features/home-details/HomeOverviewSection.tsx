import { AlertTriangle } from "lucide-react";
import type { HomeLiveStatus } from "../../shared/types/home";
import { ProgressBar } from "../../shared/components/ProgressBar";
import { formatCurrency, formatEnergy } from "../../shared/utils/format";

export function HomeOverviewSection({ home }: { home: HomeLiveStatus }) {
  return (
    <div className="flex flex-col gap-3">
      {home.penaltyActive && (
        <div className="flex items-center gap-2 rounded-input border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden="true" />
          Bu ev şu anda ceza tarifesi altında.
        </div>
      )}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="rounded-input bg-surface-subtle p-3">
          <p className="text-xs font-medium text-text-secondary">Güncel Enerji</p>
          <p className="tabular-nums mt-1 text-2xl font-semibold tracking-tight text-text-primary">
            {formatEnergy(home.currentEnergyKwh)}
          </p>
        </div>
        <div className="rounded-input bg-surface-subtle p-3">
          <p className="text-xs font-medium text-text-secondary">Güncel Maliyet</p>
          <p className="tabular-nums mt-1 text-2xl font-semibold tracking-tight text-text-primary">
            {formatCurrency(home.currentCost)}
          </p>
        </div>
        <div className="col-span-2 flex flex-col justify-center gap-2 rounded-input bg-surface-subtle p-3 sm:col-span-2">
          <ProgressBar label="Enerji kotası" percentage={home.energyQuotaPercentage} />
          <ProgressBar label="Bütçe kotası" percentage={home.budgetQuotaPercentage} />
        </div>
      </div>
    </div>
  );
}
