import { formatPercentage } from "../utils/format";
import { getQuotaTone, type QuotaTone } from "../utils/homeStatus";

interface ProgressBarProps {
  label: string;
  percentage: string | number;
}

const FILL_CLASS: Record<QuotaTone, string> = {
  normal: "bg-success",
  warning: "bg-warning",
  critical: "bg-danger",
};

const TEXT_CLASS: Record<QuotaTone, string> = {
  normal: "text-success",
  warning: "text-warning",
  critical: "text-danger",
};

export function ProgressBar({ label, percentage }: ProgressBarProps) {
  const value = Number(percentage);
  const tone = getQuotaTone(percentage);
  const width = Math.min(Math.max(Number.isFinite(value) ? value : 0, 0), 100);

  return (
    <div className="flex flex-col gap-1.5" data-testid={`progress-bar-${tone}`}>
      <div className="flex items-baseline justify-between text-xs font-medium text-text-secondary">
        <span>{label}</span>
        <span className={`tabular-nums ${TEXT_CLASS[tone]}`}>{formatPercentage(percentage)}</span>
      </div>
      <div
        className="h-2 overflow-hidden rounded-full bg-surface-subtle"
        role="progressbar"
        aria-label={label}
        aria-valuenow={Number.isFinite(value) ? value : 0}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          className={`h-full rounded-full transition-[width] duration-500 ease-out ${FILL_CLASS[tone]}`}
          style={{ width: `${width}%` }}
        />
      </div>
    </div>
  );
}
