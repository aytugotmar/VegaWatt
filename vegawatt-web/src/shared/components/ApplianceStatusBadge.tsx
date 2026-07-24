import { AlertTriangle, CheckCircle2, Clock, WifiOff, type LucideIcon } from "lucide-react";
import type { ApplianceLiveStatus } from "../types/home";
import { getApplianceHealthLabel, getApplianceHealthTone, type ApplianceHealthTone } from "../utils/homeStatus";

const TONE_ICON: Record<ApplianceHealthTone, LucideIcon> = {
  offline: WifiOff,
  stale: Clock,
  anomalous: AlertTriangle,
  warning: AlertTriangle,
  normal: CheckCircle2,
};

const TONE_CLASSES: Record<ApplianceHealthTone, string> = {
  offline: "bg-danger-soft text-danger",
  stale: "bg-warning-soft text-warning",
  anomalous: "bg-danger-soft text-danger",
  warning: "bg-warning-soft text-warning",
  normal: "bg-success-soft text-success",
};

type ApplianceStatusInput = Pick<
  ApplianceLiveStatus,
  "telemetryHealthStatus" | "anomalous" | "standbyAnomalyActive"
>;

interface ApplianceStatusBadgeProps {
  appliance: ApplianceStatusInput;
  className?: string;
}

export function ApplianceStatusBadge({ appliance, className = "" }: ApplianceStatusBadgeProps) {
  const tone = getApplianceHealthTone(appliance);
  const Icon = TONE_ICON[tone];
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-semibold ${TONE_CLASSES[tone]} ${className}`}
      data-testid={tone === "anomalous" ? "anomaly-badge" : "status-badge"}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden="true" />
      {getApplianceHealthLabel(tone)}
    </span>
  );
}
