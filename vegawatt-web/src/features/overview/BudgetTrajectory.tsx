import { formatCurrency, formatPercentage } from "../../shared/utils/format";
import { getQuotaTone } from "../../shared/utils/homeStatus";
import { useLanguage } from "../../shared/i18n/LanguageContext";

interface BudgetTrajectoryProps {
  totalCurrentCost: number;
  maxBudgetQuotaPercentage: number;
  projectedMonthEndCost: number | null;
}

const TONE_CLASS: Record<"normal" | "warning" | "critical", string> = {
  normal: "text-text-primary",
  warning: "text-warning",
  critical: "text-danger",
};

const BAR_TONE_CLASS: Record<"normal" | "warning" | "critical", string> = {
  normal: "bg-primary",
  warning: "bg-warning",
  critical: "bg-danger",
};

export function BudgetTrajectory({
  totalCurrentCost,
  maxBudgetQuotaPercentage,
  projectedMonthEndCost,
}: BudgetTrajectoryProps) {
  const { t } = useLanguage();
  const tone = getQuotaTone(maxBudgetQuotaPercentage);

  return (
    <div className="card-glass flex h-full flex-col gap-1 rounded-input border border-border p-6">
      <span className="text-xs font-medium uppercase tracking-wide text-text-muted">{t("overview.budgetPacing")}</span>
      <p className={`text-3xl font-semibold tracking-tight tabular-nums ${TONE_CLASS[tone]}`}>
        {formatCurrency(totalCurrentCost)}
      </p>

      <div className="mt-2 flex items-center justify-between text-xs text-text-muted">
        <span>{t("overview.budgetUsage")}</span>
        <span className="font-medium text-text-secondary">{formatPercentage(maxBudgetQuotaPercentage)}</span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-surface-subtle">
        <div
          className={`h-full rounded-full transition-all ${BAR_TONE_CLASS[tone]}`}
          style={{ width: `${Math.min(100, Math.max(0, maxBudgetQuotaPercentage))}%` }}
        />
      </div>

      {projectedMonthEndCost !== null && (
        <div className="mt-auto pt-3 text-xs text-text-secondary">
          {t("overview.monthEndEstimate")}
          <p className="mt-0.5 text-base font-semibold tabular-nums text-text-primary">
            {formatCurrency(projectedMonthEndCost)}
          </p>
        </div>
      )}
    </div>
  );
}
