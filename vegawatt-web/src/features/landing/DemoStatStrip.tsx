import { AlertTriangle } from "lucide-react";

/**
 * Static demo numbers for marketing surfaces (landing hero, auth panels) — intentionally not
 * wired to any API. Keeps a consistent "this is what the product looks like" story everywhere
 * a visitor hasn't logged in yet.
 */
export function DemoStatStrip() {
  return (
    <div className="rounded-input border border-border bg-surface/60 px-4 py-3">
      <div className="mb-2 flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Demo Verisi · Merkez Ev</span>
        <span className="flex items-center gap-1 text-xs font-medium text-success">
          <span className="h-1.5 w-1.5 rounded-full bg-success" aria-hidden="true" />
          Canlı
        </span>
      </div>
      <div className="flex items-baseline gap-4">
        <div>
          <p className="text-2xl font-semibold tabular-nums text-text-primary">4.28 kW</p>
          <p className="text-xs text-text-muted">Şu anki güç</p>
        </div>
        <div>
          <p className="text-2xl font-semibold tabular-nums text-text-primary">₺734</p>
          <p className="text-xs text-text-muted">Bu ay</p>
        </div>
        <div className="flex items-center gap-1 text-warning">
          <AlertTriangle className="h-4 w-4" aria-hidden="true" />
          <span className="text-sm font-medium">1 Anomali</span>
        </div>
      </div>
    </div>
  );
}
