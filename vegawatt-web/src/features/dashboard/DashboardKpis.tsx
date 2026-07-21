import { AlertTriangle, Gauge, Home, Wallet, type LucideIcon } from "lucide-react";
import type { HomeLiveSummary } from "../../shared/types/home";
import { formatCurrency, formatEnergy, toSafeNumber } from "../../shared/utils/format";

interface DashboardKpisProps {
  homes: HomeLiveSummary[];
}

interface Kpi {
  label: string;
  value: string;
  icon: LucideIcon;
}

function needsAttention(home: HomeLiveSummary): boolean {
  return (
    home.penaltyActive ||
    toSafeNumber(home.energyQuotaPercentage) >= 80 ||
    toSafeNumber(home.budgetQuotaPercentage) >= 80
  );
}

export function DashboardKpis({ homes }: DashboardKpisProps) {
  const totalEnergy = homes.reduce((sum, home) => sum + toSafeNumber(home.currentEnergyKwh), 0);
  const totalCost = homes.reduce((sum, home) => sum + toSafeNumber(home.currentCost), 0);
  const attentionCount = homes.filter(needsAttention).length;

  const kpis: Kpi[] = [
    { label: "Kayıtlı Evler", value: String(homes.length), icon: Home },
    { label: "Toplam Güncel Enerji", value: formatEnergy(totalEnergy), icon: Gauge },
    { label: "Toplam Güncel Maliyet", value: formatCurrency(totalCost), icon: Wallet },
    { label: "Dikkat Gerektiren Evler", value: String(attentionCount), icon: AlertTriangle },
  ];

  return (
    <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
      {kpis.map((kpi) => (
        <div key={kpi.label} className="rounded-card border border-border bg-surface p-4 shadow-[var(--shadow-card)]">
          <kpi.icon className="h-4 w-4 text-text-muted" aria-hidden="true" />
          <p className="tabular-nums mt-2.5 text-xl font-semibold tracking-tight text-text-primary">{kpi.value}</p>
          <p className="text-xs font-medium text-text-secondary">{kpi.label}</p>
        </div>
      ))}
    </div>
  );
}
