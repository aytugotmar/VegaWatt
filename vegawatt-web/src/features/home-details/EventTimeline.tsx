import { AlertTriangle, CheckCircle2, Clock, History, TrendingUp, WifiOff, type LucideIcon } from "lucide-react";
import type { ApplianceLiveStatus, OperationalEvent } from "../../shared/types/home";
import { EmptyState } from "../../shared/components/EmptyState";
import { getEventTone, getEventTypeLabel, type EventTone } from "../../shared/utils/eventLabels";
import { formatDateTime } from "../../shared/utils/format";

const EVENT_ICON: Record<string, LucideIcon> = {
  APPLIANCE_ANOMALY_DETECTED: AlertTriangle,
  APPLIANCE_STANDBY_ANOMALY_DETECTED: AlertTriangle,
  APPLIANCE_ANOMALY_RECOVERED: CheckCircle2,
  APPLIANCE_STANDBY_ANOMALY_RECOVERED: CheckCircle2,
  APPLIANCE_TELEMETRY_RESUMED: CheckCircle2,
  APPLIANCE_TELEMETRY_STALE: Clock,
  APPLIANCE_TELEMETRY_OFFLINE: WifiOff,
  ENERGY_QUOTA_80_REACHED: TrendingUp,
  ENERGY_QUOTA_100_REACHED: TrendingUp,
  BUDGET_QUOTA_80_REACHED: TrendingUp,
  BUDGET_QUOTA_100_REACHED: TrendingUp,
  PENALTY_TARIFF_ACTIVATED: TrendingUp,
};

const TONE_ICON_CLASS: Record<EventTone, string> = {
  danger: "bg-danger-soft text-danger",
  warning: "bg-warning-soft text-warning",
  success: "bg-success-soft text-success",
  info: "bg-info-soft text-info",
};

interface EventTimelineProps {
  events: OperationalEvent[];
  appliances: ApplianceLiveStatus[];
}

export function EventTimeline({ events, appliances }: EventTimelineProps) {
  if (events.length === 0) {
    return (
      <EmptyState
        icon={History}
        title="Henüz olay yok"
        description="Bu ev için sistem henüz bir olay kaydetmedi."
      />
    );
  }

  return (
    <ul className="flex flex-col gap-2">
      {events.map((event) => {
        const Icon = EVENT_ICON[event.eventType] ?? AlertTriangle;
        const tone = getEventTone(event.eventType);
        const applianceName = appliances.find((appliance) => appliance.applianceId === event.applianceId)
          ?.applianceName;

        return (
          <li key={event.id} className="flex gap-3 rounded-card border border-border bg-surface p-3">
            <span
              className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full ${TONE_ICON_CLASS[tone]}`}
            >
              <Icon className="h-3.5 w-3.5" aria-hidden="true" />
            </span>
            <div className="flex min-w-0 flex-1 flex-col gap-0.5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-sm font-medium text-text-primary">
                  {getEventTypeLabel(event.eventType)}
                  {applianceName && <span className="font-normal text-text-secondary"> · {applianceName}</span>}
                </span>
                <span className="shrink-0 text-xs text-text-muted">{formatDateTime(event.eventTime)}</span>
              </div>
              {event.details && <p className="text-xs text-text-secondary">{event.details}</p>}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
