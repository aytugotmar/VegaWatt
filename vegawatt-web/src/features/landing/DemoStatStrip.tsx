import { AlertTriangle, Gauge, Wallet } from "lucide-react";
import { RadialGauge } from "../../shared/components/RadialGauge";

/**
 * Static demo numbers for marketing surfaces (landing hero, auth panels) — intentionally not
 * wired to any API. Doubles as a mini product preview so a visitor sees roughly what the app's
 * live home view looks like before signing up.
 */
export function DemoStatStrip() {
  return (
    <div className="glow-card rounded-modal border border-border bg-surface/80 p-4 backdrop-blur-sm">
      <div className="mb-3 flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Demo Verisi · Merkez Ev</p>
          <p className="text-[11px] text-text-muted">Uygulamada göreceğiniz canlı görünüm budur</p>
        </div>
        <span className="flex items-center gap-1 text-xs font-medium text-success">
          <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-success" aria-hidden="true" />
          Canlı
        </span>
      </div>

      <div className="flex items-center gap-4">
        <RadialGauge percentage={49} size={72} strokeWidth={6} />
        <div className="flex flex-1 flex-col gap-2.5">
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-info-soft text-info">
              <Gauge className="h-4 w-4" aria-hidden="true" />
            </span>
            <div className="leading-tight">
              <p className="text-lg font-bold text-text-primary">4.28 kW</p>
              <p className="text-[11px] text-text-muted">Şu anki güç</p>
            </div>
          </div>
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-warning-soft text-warning">
              <Wallet className="h-4 w-4" aria-hidden="true" />
            </span>
            <div className="leading-tight">
              <p className="text-lg font-bold text-text-primary">₺734</p>
              <p className="text-[11px] text-text-muted">Bu ay · bütçenin %49'u</p>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-3 flex items-center gap-2 rounded-input bg-danger-soft px-3 py-2 text-danger">
        <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden="true" />
        <span className="text-xs font-medium">1 cihazda anomali tespit edildi — anında bildirim gönderildi</span>
      </div>
    </div>
  );
}
