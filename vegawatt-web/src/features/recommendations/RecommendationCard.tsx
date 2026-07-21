import { Gauge, Leaf, Lightbulb, Mail, MailWarning, MailX, MessageSquareText, type LucideIcon } from "lucide-react";
import type { Recommendation } from "../../shared/types/home";
import { getAdvisoryTriggerExplanation, getAdvisoryTriggerLabel } from "../../shared/utils/advisoryLabels";
import { formatRelativeTime } from "../../shared/utils/format";

const TRIGGER_ICON: Record<string, LucideIcon> = {
  QUOTA_80: Gauge,
  QUOTA_100: Gauge,
  ANOMALY: Lightbulb,
  RECOVERY: Leaf,
};

const EMAIL_STATUS_CONFIG: Record<Recommendation["emailStatus"], { icon: LucideIcon; label: string; className: string }> = {
  SENT: { icon: Mail, label: "E-posta gönderildi", className: "text-success" },
  PENDING: { icon: MailWarning, label: "E-posta gönderiliyor", className: "text-text-muted" },
  FAILED: { icon: MailX, label: "E-posta gönderilemedi", className: "text-danger" },
};

interface RecommendationCardProps {
  recommendation: Recommendation;
  highlighted?: boolean;
}

export function RecommendationCard({ recommendation, highlighted = false }: RecommendationCardProps) {
  const TriggerIcon = TRIGGER_ICON[recommendation.triggerType] ?? MessageSquareText;
  const emailStatus = EMAIL_STATUS_CONFIG[recommendation.emailStatus];

  return (
    <li
      className={`flex gap-3 rounded-card border border-border p-3 ${highlighted ? "bg-primary-soft" : "bg-surface"}`}
    >
      <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-surface-subtle text-text-secondary">
        <TriggerIcon className="h-3.5 w-3.5" aria-hidden="true" />
      </span>
      <div className="flex min-w-0 flex-col gap-1.5">
        <div className="flex flex-col gap-0.5">
          <span className="text-xs font-semibold text-text-secondary">
            {getAdvisoryTriggerLabel(recommendation.triggerType)}
          </span>
          <span className="text-xs text-text-muted">{getAdvisoryTriggerExplanation(recommendation.triggerType)}</span>
        </div>
        <p className="text-sm text-text-primary">{recommendation.content}</p>
        <div className="flex flex-wrap items-center gap-2 text-xs text-text-muted">
          <span>{formatRelativeTime(recommendation.createdAt)}</span>
          {recommendation.fallbackUsed && (
            <span className="rounded-full bg-info-soft px-2 py-0.5 font-medium text-info">
              Standart sistem mesajı
            </span>
          )}
          <span className={`inline-flex items-center gap-1 font-medium ${emailStatus.className}`}>
            <emailStatus.icon className="h-3 w-3" aria-hidden="true" />
            {emailStatus.label}
          </span>
        </div>
      </div>
    </li>
  );
}
