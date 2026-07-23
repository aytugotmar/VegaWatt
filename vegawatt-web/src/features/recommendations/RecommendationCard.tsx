import { Mail, MailWarning, MailX, type LucideIcon } from "lucide-react";
import type { OperationalEvent, Recommendation } from "../../shared/types/home";
import {
  getAdvisorySeverity,
  getAdvisoryTriggerExplanation,
  getAdvisoryTriggerLabel,
  getSeverityLabel,
  type AdvisorySeverity,
} from "../../shared/utils/advisoryLabels";
import { formatDateTime } from "../../shared/utils/format";

const EMAIL_STATUS_CONFIG: Record<Recommendation["emailStatus"], { icon: LucideIcon; label: string; className: string }> = {
  SENT: { icon: Mail, label: "E-posta gönderildi", className: "text-success" },
  PENDING: { icon: MailWarning, label: "E-posta gönderiliyor", className: "text-text-muted" },
  FAILED: { icon: MailX, label: "E-posta gönderilemedi", className: "text-danger" },
};

const SEVERITY_BORDER_CLASS: Record<AdvisorySeverity, string> = {
  CRITICAL: "border-l-danger",
  WARNING: "border-l-warning",
  INFO: "border-l-info",
};

const SEVERITY_BADGE_CLASS: Record<AdvisorySeverity, string> = {
  CRITICAL: "bg-danger-soft text-danger",
  WARNING: "bg-warning-soft text-warning",
  INFO: "bg-info-soft text-info",
};

interface RecommendationCardProps {
  recommendation: Recommendation;
  highlighted?: boolean;
  /** The `OperationalEvent` that triggered this recommendation, if it could still be found (an
   * old recommendation's event may no longer be resolvable). Shown as historical, point-in-time
   * evidence — never today's live appliance numbers, which could misrepresent an old
   * recommendation (see `advisoryLabels.ts`). */
  linkedEvent?: OperationalEvent;
}

export function RecommendationCard({ recommendation, highlighted = false, linkedEvent }: RecommendationCardProps) {
  const severity = getAdvisorySeverity(recommendation.triggerType);
  const emailStatus = EMAIL_STATUS_CONFIG[recommendation.emailStatus];

  return (
    <li
      className={`flex flex-col gap-2 rounded-card border border-l-4 border-border p-3.5 ${SEVERITY_BORDER_CLASS[severity]} ${highlighted ? "bg-primary-soft" : "bg-surface"}`}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span
          className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ${SEVERITY_BADGE_CLASS[severity]}`}
        >
          {getSeverityLabel(severity)} · {getAdvisoryTriggerLabel(recommendation.triggerType)}
        </span>
        <span className="text-xs text-text-muted">{formatDateTime(recommendation.createdAt)}</span>
      </div>

      <div className="rounded-input bg-surface-subtle p-2.5 text-xs text-text-secondary">
        <p className="mb-1 font-semibold uppercase tracking-wide text-text-muted">
          Neden bu öneriyi görüyorsunuz?
        </p>
        <p>{getAdvisoryTriggerExplanation(recommendation.triggerType)}</p>
        {linkedEvent && (
          <p className="mt-1.5 border-t border-border pt-1.5 text-text-secondary">
            {formatDateTime(linkedEvent.eventTime)} — {linkedEvent.details}
          </p>
        )}
      </div>

      <p className="text-sm leading-relaxed text-text-primary">{recommendation.content}</p>

      <div className="flex flex-wrap items-center gap-2 text-xs text-text-muted">
        {recommendation.fallbackUsed && (
          <span className="rounded-full bg-info-soft px-2 py-0.5 font-medium text-info">Otomatik güvenli öneri</span>
        )}
        <span className={`inline-flex items-center gap-1 font-medium ${emailStatus.className}`}>
          <emailStatus.icon className="h-3 w-3" aria-hidden="true" />
          {emailStatus.label}
        </span>
      </div>
    </li>
  );
}
