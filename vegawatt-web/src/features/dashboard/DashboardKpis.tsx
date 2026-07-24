import { AlertTriangle, Gauge, Home, Wallet, type LucideIcon } from "lucide-react";
import type { HomeLiveSummary } from "../../shared/types/home";
import { formatCurrency, formatEnergy, toSafeNumber } from "../../shared/utils/format";
import { TONE_BADGE_CLASSES, type Tone } from "../../shared/utils/toneClasses";

interface DashboardKpisProps {
  homes: HomeLiveSummary[];
}

interface Kpi {
  label: string;
  value: string;
  icon: LucideIcon;
  tone: Tone;
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
    { label: "Kayıtlı Evler", value: String(homes.length), icon: Home, tone: "primary" },
    { label: "Toplam Güncel Enerji", value: formatEnergy(totalEnergy), icon: Gauge, tone: "info" },
    { label: "Toplam Güncel Maliyet", value: formatCurrency(totalCost), icon: Wallet, tone: "warning" },
    {
      label: "Dikkat Gerektiren Evler",
      value: String(attentionCount),
      icon: AlertTriangle,
      tone: attentionCount > 0 ? "danger" : "success",
    },
  ];

  return (
    <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
      {kpis.map((kpi) => (
        <div
          key={kpi.label}
          className="rounded-card border border-border bg-surface p-4 shadow-[var(--shadow-card)]"
        >
          <span className={`flex h-9 w-9 items-center justify-center rounded-full ${TONE_BADGE_CLASSES[kpi.tone]}`}>
            <kpi.icon className="h-4 w-4" aria-hidden="true" />
          </span>
          <p className="mt-3 text-2xl font-bold tracking-tight text-text-primary">{kpi.value}</p>
          <p className="text-xs font-medium text-text-secondary">{kpi.label}</p>
        </div>
      ))}
    </div>
  );
}
