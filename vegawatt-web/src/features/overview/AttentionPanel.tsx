import { AlertTriangle, CheckCircle2 } from "lucide-react";
import { Link } from "react-router-dom";
import type { AttentionItem } from "./useOverviewData";

export function AttentionPanel({ items }: { items: AttentionItem[] }) {
  return (
    <div className="card-glass flex flex-col gap-2 rounded-input border border-border p-5">
      <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Dikkat Gerektirenler</span>
      {items.length === 0 ? (
        <div className="flex items-center gap-2 py-2 text-sm text-text-secondary">
          <CheckCircle2 className="h-4 w-4 text-success" aria-hidden="true" />
          Her şey normal görünüyor.
        </div>
      ) : (
        <ul className="flex flex-col gap-2">
          {items.map((item, index) => (
            <li key={`${item.homeId}-${index}`}>
              <Link
                to={`/app/homes/${item.homeId}`}
                className="flex items-center gap-2 text-sm text-text-primary hover:text-primary"
              >
                <AlertTriangle className="h-4 w-4 shrink-0 text-warning" aria-hidden="true" />
                <span className="truncate">
                  {item.homeName} · {item.reason}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
