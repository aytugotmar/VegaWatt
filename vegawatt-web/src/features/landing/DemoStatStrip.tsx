import { AlertTriangle, Fan, Gauge, Refrigerator, Wallet, WashingMachine, type LucideIcon } from "lucide-react";
import { RadialGauge } from "../../shared/components/RadialGauge";

interface DemoDevice {
  name: string;
  icon: LucideIcon;
  power: string;
  anomalous: boolean;
}

const DEMO_DEVICES: DemoDevice[] = [
  { name: "Buzdolabı", icon: Refrigerator, power: "145 W", anomalous: false },
  { name: "Klima", icon: Fan, power: "1.240 W", anomalous: true },
  { name: "Çamaşır Makinesi", icon: WashingMachine, power: "610 W", anomalous: false },
];

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

      <div className="mt-3 flex flex-col gap-1.5">
        <p className="text-[11px] font-semibold uppercase tracking-wide text-text-muted">Cihazlar</p>
        {DEMO_DEVICES.map((device) => (
          <div
            key={device.name}
            className="flex items-center justify-between gap-2 rounded-input bg-surface-subtle px-2.5 py-1.5"
          >
            <div className="flex min-w-0 items-center gap-2">
              <device.icon className="h-3.5 w-3.5 shrink-0 text-text-secondary" aria-hidden="true" />
              <span className="truncate text-xs font-medium text-text-primary">{device.name}</span>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <span className="tabular-nums text-xs text-text-secondary">{device.power}</span>
              <span
                className={`rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                  device.anomalous ? "bg-danger-soft text-danger" : "bg-success-soft text-success"
                }`}
              >
                {device.anomalous ? "Anomali" : "Normal"}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
