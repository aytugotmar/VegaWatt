import { Eye, Plus } from "lucide-react";
import { getApplianceCatalogIcon } from "../../../shared/constants/applianceCatalogIcons";
import { getApplianceCategoryLabel } from "../../../shared/constants/applianceCategoryLabels";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import { getBehaviorProfileLabel, getTriggerTypeLabel } from "../model/applianceCatalogLabels";

const MAX_VISIBLE_TRIGGERS = 2;

interface ApplianceCatalogCardProps {
  item: ApplianceCatalogItem;
  onOpenDetails: () => void;
  onAdd: () => void;
}

export function ApplianceCatalogCard({ item, onOpenDetails, onAdd }: ApplianceCatalogCardProps) {
  const Icon = getApplianceCatalogIcon(item.iconKey);
  const visibleTriggers = item.supportedTriggers.slice(0, MAX_VISIBLE_TRIGGERS);

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onOpenDetails}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          onOpenDetails();
        }
      }}
      className="flex cursor-pointer flex-col gap-2 rounded-card border border-border bg-surface p-3 text-left transition hover:border-primary/50"
    >
      <div className="flex items-start gap-2.5">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-input bg-surface-subtle text-text-secondary">
          <Icon className="h-5 w-5" aria-hidden="true" />
        </span>
        <span className="flex-1">
          <span className="block text-sm font-semibold text-text-primary">{item.displayName}</span>
          <span className="block text-[11px] text-text-muted">{getApplianceCategoryLabel(item.category)}</span>
        </span>
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onAdd();
          }}
          aria-label={`${item.displayName} ekle`}
          className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary text-white transition hover:bg-primary-hover"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <p className="text-xs text-text-secondary">{getBehaviorProfileLabel(item.behaviorProfile)}</p>
      <p className="text-[11px] text-text-muted">
        Tipik kullanım: {item.defaultActiveMinWatt}–{item.defaultActiveMaxWatt} W
      </p>

      {(item.supportsStandby || visibleTriggers.length > 0) && (
        <div className="flex flex-wrap gap-1">
          {item.supportsStandby && (
            <span className="rounded-full bg-primary-soft px-2 py-0.5 text-[10px] font-medium text-primary">
              Bekleme modu
            </span>
          )}
          {visibleTriggers.map((trigger) => (
            <span
              key={trigger}
              className="inline-flex items-center gap-1 rounded-full bg-surface-subtle px-2 py-0.5 text-[10px] font-medium text-text-secondary"
            >
              <Eye className="h-2.5 w-2.5" aria-hidden="true" />
              {getTriggerTypeLabel(trigger)}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
