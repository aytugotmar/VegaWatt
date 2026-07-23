import { Link } from "react-router-dom";
import { getApplianceIcon } from "../../shared/constants/applianceTypes";
import { formatPercentage, formatPower } from "../../shared/utils/format";
import type { TopConsumer } from "./useOverviewData";

interface TopConsumersPanelProps {
  consumers: TopConsumer[];
  /** Set false when already scoped to a single home (e.g. that home's own detail page) — hides
   * the redundant home name and the link back to a home the caller is already viewing. */
  linkToHomes?: boolean;
}

export function TopConsumersPanel({ consumers, linkToHomes = true }: TopConsumersPanelProps) {
  return (
    <div className="flex flex-col gap-3 rounded-input border border-border bg-surface p-5">
      <span className="text-xs font-medium uppercase tracking-wide text-text-muted">En Çok Tüketen Cihazlar</span>
      {consumers.length === 0 ? (
        <p className="py-2 text-sm text-text-muted">Henüz veri yok.</p>
      ) : (
        <ol className="flex flex-col gap-3">
          {consumers.map((consumer) => {
            const Icon = getApplianceIcon(consumer.appliance.applianceType);
            const nameBlock = (
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-text-primary">{consumer.appliance.applianceName}</p>
                {linkToHomes && <p className="truncate text-xs text-text-muted">{consumer.homeName}</p>}
              </div>
            );

            return (
              <li key={consumer.appliance.applianceId} className="flex flex-col gap-1.5">
                <div className="flex items-center gap-3">
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-input bg-surface-subtle text-text-secondary">
                    <Icon className="h-4 w-4" aria-hidden="true" />
                  </span>
                  {linkToHomes ? (
                    <Link to={`/app/homes/${consumer.homeId}`} className="flex min-w-0 flex-1 hover:text-primary">
                      {nameBlock}
                    </Link>
                  ) : (
                    nameBlock
                  )}
                  <div className="shrink-0 text-right">
                    <p className="tabular-nums text-sm font-medium text-text-primary">
                      {formatPower(consumer.appliance.currentPowerWatt)}
                    </p>
                    <p className="tabular-nums text-xs text-text-muted">
                      {formatPercentage(consumer.shareOfTotal * 100)}
                    </p>
                  </div>
                </div>
                <div className="ml-12 h-1 overflow-hidden rounded-full bg-surface-subtle">
                  <div
                    className="h-full rounded-full bg-primary"
                    style={{ width: `${Math.min(100, consumer.shareOfTotal * 100)}%` }}
                  />
                </div>
              </li>
            );
          })}
        </ol>
      )}
    </div>
  );
}
