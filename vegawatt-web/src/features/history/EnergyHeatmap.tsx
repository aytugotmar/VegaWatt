import { Fragment, useMemo } from "react";
import type { ConsumptionHistoryPoint } from "../../shared/types/home";
import { toSafeNumber } from "../../shared/utils/format";

const DAY_LABELS = ["Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"];
const MIN_POINTS_FOR_HEATMAP = 2;

function bucketizeByDayAndHour(points: ConsumptionHistoryPoint[]): number[][] {
  const grid: number[][] = Array.from({ length: 7 }, () => Array(24).fill(0));
  const sorted = [...points].sort(
    (a, b) => new Date(a.snapshotTime).getTime() - new Date(b.snapshotTime).getTime(),
  );

  // Snapshots store the home's cumulative energy, not an interval delta — the delta between
  // consecutive snapshots is what actually happened in that hour. Clamped to 0 because a
  // rollover or a compensating write could otherwise show as negative consumption.
  for (let i = 1; i < sorted.length; i++) {
    const delta = Math.max(0, toSafeNumber(sorted[i].energyKwh) - toSafeNumber(sorted[i - 1].energyKwh));
    const at = new Date(sorted[i].snapshotTime);
    const mondayFirstDay = (at.getDay() + 6) % 7;
    grid[mondayFirstDay][at.getHours()] += delta;
  }

  return grid;
}

/** A 7-day × 24-hour usage heatmap built entirely client-side from the same raw history points
 * the line chart already fetches — no backend aggregation endpoint required. One hue (the
 * energy token), varying only in opacity, so it reads as magnitude rather than category. */
export function EnergyHeatmap({ points }: { points: ConsumptionHistoryPoint[] }) {
  const grid = useMemo(() => bucketizeByDayAndHour(points), [points]);
  const maxValue = useMemo(() => Math.max(0.000001, ...grid.flat()), [grid]);

  if (points.length < MIN_POINTS_FOR_HEATMAP) {
    return <p className="text-sm text-text-muted">Isı haritası için yeterli geçmiş veri yok.</p>;
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto">
        <div className="inline-grid gap-0.5" style={{ gridTemplateColumns: "28px repeat(24, minmax(14px, 1fr))" }}>
          <div aria-hidden="true" />
          {Array.from({ length: 24 }, (_, hour) => (
            <div key={hour} className="text-center text-[10px] text-text-muted">
              {hour % 3 === 0 ? hour : ""}
            </div>
          ))}
          {grid.map((row, dayIndex) => (
            <Fragment key={DAY_LABELS[dayIndex]}>
              <div className="flex items-center text-xs text-text-muted">{DAY_LABELS[dayIndex]}</div>
              {row.map((value, hour) => {
                const intensity = value > 0 ? Math.max(0.08, value / maxValue) : 0;
                return (
                  <div
                    key={hour}
                    title={`${DAY_LABELS[dayIndex]} ${String(hour).padStart(2, "0")}:00 — ${value.toLocaleString("tr-TR", { maximumFractionDigits: 3 })} kWh`}
                    className="relative aspect-square rounded-[2px] bg-surface-subtle"
                  >
                    {intensity > 0 && (
                      <div
                        className="absolute inset-0 rounded-[2px] bg-energy"
                        style={{ opacity: intensity }}
                        aria-hidden="true"
                      />
                    )}
                  </div>
                );
              })}
            </Fragment>
          ))}
        </div>
      </div>
      <div className="flex items-center gap-2 text-[11px] text-text-muted">
        <span>Az</span>
        <div className="h-2 max-w-[120px] flex-1 rounded-full bg-gradient-to-r from-surface-subtle to-energy" />
        <span>Çok</span>
      </div>
    </div>
  );
}
