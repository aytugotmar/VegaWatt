import { CheckCircle2, X } from "lucide-react";
import { useId } from "react";
import { Button } from "../../../shared/components/Button";
import { Dialog } from "../../../shared/components/Dialog";
import { getApplianceCatalogIcon } from "../../../shared/constants/applianceCatalogIcons";
import { getApplianceCategoryLabel } from "../../../shared/constants/applianceCategoryLabels";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import { getBehaviorProfileLabel, getTriggerTypeLabel } from "../model/applianceCatalogLabels";

interface ApplianceCatalogDetailsDrawerProps {
  item: ApplianceCatalogItem;
  onClose: () => void;
  onAdd: () => void;
}

/** Reuses the shared `Dialog` shell (focus trap, Escape-to-close, aria-labelledby, Tab wrap) rather
 * than reimplementing accessible drawer/modal mechanics — yapılacak.md §20.5's requirements are
 * already fully met by that component. */
export function ApplianceCatalogDetailsDrawer({ item, onClose, onAdd }: ApplianceCatalogDetailsDrawerProps) {
  const titleId = useId();
  const Icon = getApplianceCatalogIcon(item.iconKey);
  const hasStandby = item.supportsStandby && item.defaultStandbyMinWatt !== null && item.defaultStandbyMaxWatt !== null;

  return (
    <Dialog open onClose={onClose} labelledBy={titleId} maxWidthClassName="max-w-md">
      <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4">
        <div className="flex items-center gap-2.5">
          <span className="flex h-9 w-9 items-center justify-center rounded-input bg-surface-subtle text-text-secondary">
            <Icon className="h-4.5 w-4.5" aria-hidden="true" />
          </span>
          <h2 id={titleId} className="text-base font-semibold text-text-primary">
            {item.displayName}
          </h2>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Kapat"
          className="flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition hover:bg-surface-subtle hover:text-text-primary"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <div className="flex flex-col gap-3 overflow-y-auto px-5 py-4">
        <DetailRow label="Kategori" value={getApplianceCategoryLabel(item.category)} />
        <DetailRow label="Kullanım biçimi" value={getBehaviorProfileLabel(item.behaviorProfile)} />
        <DetailRow label="Tipik tüketim" value={`${item.defaultActiveMinWatt}–${item.defaultActiveMaxWatt} W`} />
        {hasStandby && (
          <DetailRow label="Bekleme tüketimi" value={`${item.defaultStandbyMinWatt}–${item.defaultStandbyMaxWatt} W`} />
        )}
        <DetailRow label="Varsayılan güvenli sınır" value={`${item.defaultSafePowerLimitWatt} W`} />

        {item.supportedTriggers.length > 0 && (
          <div>
            <h3 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-text-secondary">
              İzlenen durumlar
            </h3>
            <ul className="flex flex-col gap-1">
              {item.supportedTriggers.map((trigger) => (
                <li key={trigger} className="flex items-center gap-1.5 text-sm text-text-primary">
                  <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-success" aria-hidden="true" />
                  {getTriggerTypeLabel(trigger)}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="border-t border-border px-5 py-3">
        <Button variant="primary" onClick={onAdd} className="w-full">
          Bu cihazı ekle
        </Button>
      </div>
    </Dialog>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 text-sm">
      <span className="text-text-secondary">{label}</span>
      <span className="font-medium text-text-primary">{value}</span>
    </div>
  );
}
