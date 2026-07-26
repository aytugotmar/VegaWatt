import { AlertTriangle, CheckCircle2, TrendingUp, XCircle, type LucideIcon } from "lucide-react";
import type { HomeHealthStatus } from "../utils/homeStatus";
import { getHomeStatusLabel } from "../utils/homeStatus";
import { useLanguage } from "../i18n/LanguageContext";

const STATUS_ICON: Record<HomeHealthStatus, LucideIcon> = {
  NORMAL: CheckCircle2,
  WARNING: AlertTriangle,
  CRITICAL: XCircle,
  PENALTY: TrendingUp,
};

const STATUS_CLASSES: Record<HomeHealthStatus, string> = {
  NORMAL: "bg-success-soft text-success",
  WARNING: "bg-warning-soft text-warning",
  CRITICAL: "bg-danger-soft text-danger",
  PENALTY: "bg-danger-soft text-danger",
};

export function StatusBadge({ status }: { status: HomeHealthStatus }) {
  const { t } = useLanguage();
  const Icon = STATUS_ICON[status];
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${STATUS_CLASSES[status]}`}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden="true" />
      {getHomeStatusLabel(status, t)}
    </span>
  );
}
