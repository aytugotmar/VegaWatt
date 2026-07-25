import { LineChart as LineChartIcon } from "lucide-react";
import { useMemo, useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { ConsumptionHistoryPoint } from "../../shared/types/home";
import { EmptyState } from "../../shared/components/EmptyState";
import { useTheme } from "../../shared/hooks/useTheme";
import { formatDateTime, toSafeNumber } from "../../shared/utils/format";

type MetricKey = "energy" | "cost";

const SERIES_COLOR: Record<MetricKey, { light: string; dark: string }> = {
  energy: { light: "#2F6FED", dark: "#6AB4FF" },
  cost: { light: "#C6821A", dark: "#F0B25E" },
};

const INK = {
  muted: { light: "#8891ac", dark: "#667195" },
  grid: { light: "#dde3ef", dark: "#24304f" },
  surface: { light: "#ffffff", dark: "#121a38" },
  secondary: { light: "#5b6785", dark: "#9aa7c7" },
  danger: { light: "#d1453a", dark: "#ff7a6b" },
};

interface ChartPoint {
  time: string;
  energyKwh: number;
  cost: number;
  penalty: boolean;
}

/**
 * Snapshots store cumulative energy/cost, not the amount used in that interval — the delta
 * between consecutive snapshots is what actually happened, and is what reveals when consumption
 * spiked (a raw cumulative line only ever goes up, which hides that story). Clamped to 0 because
 * a billing rollover or compensating write could otherwise show as negative consumption.
 */
function toIntervalDeltas(points: ConsumptionHistoryPoint[]): ChartPoint[] {
  const sorted = [...points].sort((a, b) => new Date(a.snapshotTime).getTime() - new Date(b.snapshotTime).getTime());
  const result: ChartPoint[] = [];
  for (let i = 1; i < sorted.length; i++) {
    const previous = sorted[i - 1];
    const current = sorted[i];
    result.push({
      time: formatDateTime(current.snapshotTime),
      energyKwh: Math.max(0, toSafeNumber(current.energyKwh) - toSafeNumber(previous.energyKwh)),
      cost: Math.max(0, toSafeNumber(current.cost) - toSafeNumber(previous.cost)),
      penalty: current.tariffState === "PENALTY",
    });
  }
  return result;
}

function CustomTooltip({
  active,
  payload,
  metric,
  surfaceColor,
  secondaryColor,
}: {
  active?: boolean;
  payload?: { payload: ChartPoint }[];
  metric: MetricKey;
  surfaceColor: string;
  secondaryColor: string;
}) {
  if (!active || !payload?.length) return null;
  const point = payload[0].payload;
  return (
    <div className="rounded-input border border-border px-3 py-2 text-xs shadow-[var(--shadow-card-hover)]" style={{ background: surfaceColor }}>
      <p className="font-semibold text-text-primary">{point.time}</p>
      <p className="tabular-nums" style={{ color: secondaryColor }}>
        Bu aralıkta enerji: {point.energyKwh.toLocaleString("tr-TR", { maximumFractionDigits: 3 })} kWh
      </p>
      <p className="tabular-nums" style={{ color: secondaryColor }}>
        Bu aralıkta maliyet: {point.cost.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} TRY
      </p>
      <p style={{ color: secondaryColor }}>{point.penalty ? "Tarife: Ceza" : "Tarife: Normal"}</p>
      {metric === "energy" ? null : null}
    </div>
  );
}

export function ConsumptionChart({ points }: { points: ConsumptionHistoryPoint[] }) {
  const { theme } = useTheme();
  const [metric, setMetric] = useState<MetricKey>("energy");

  const data = useMemo(() => toIntervalDeltas(points), [points]);

  if (points.length === 0 || data.length === 0) {
    return (
      <EmptyState
        icon={LineChartIcon}
        title="Geçmiş veri yok"
        description="Bu ev ve seçilen zaman aralığı için henüz tüketim geçmişi bulunmuyor."
      />
    );
  }

  const color = SERIES_COLOR[metric][theme];
  const gridColor = INK.grid[theme];
  const mutedColor = INK.muted[theme];
  const surfaceColor = INK.surface[theme];
  const secondaryColor = INK.secondary[theme];
  const dangerColor = INK.danger[theme];

  const values = data.map((point) => (metric === "energy" ? point.energyKwh : point.cost));
  const total = values.reduce((sum, value) => sum + value, 0);
  const average = total / values.length;
  const peak = Math.max(...values);
  const unit = metric === "energy" ? "kWh" : "TRY";

  return (
    <div className="flex flex-col gap-3" data-testid="consumption-chart">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex w-fit rounded-input border border-border bg-surface p-0.5">
          <button
            type="button"
            onClick={() => setMetric("energy")}
            aria-pressed={metric === "energy"}
            className={`rounded-[6px] px-2.5 py-1 text-xs font-medium transition ${
              metric === "energy" ? "bg-primary text-white" : "text-text-secondary hover:bg-surface-subtle"
            }`}
          >
            Enerji
          </button>
          <button
            type="button"
            onClick={() => setMetric("cost")}
            aria-pressed={metric === "cost"}
            className={`rounded-[6px] px-2.5 py-1 text-xs font-medium transition ${
              metric === "cost" ? "bg-primary text-white" : "text-text-secondary hover:bg-surface-subtle"
            }`}
          >
            Maliyet
          </button>
        </div>
        <div className="flex flex-wrap gap-4 text-xs text-text-muted">
          <span>
            Toplam <span className="font-medium text-text-primary">{total.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} {unit}</span>
          </span>
          <span>
            Ortalama <span className="font-medium text-text-primary">{average.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} {unit}</span>
          </span>
          <span>
            Tepe <span className="font-medium text-text-primary">{peak.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} {unit}</span>
          </span>
        </div>
      </div>

      <div
        role="img"
        aria-label={`${metric === "energy" ? "Enerji tüketimi" : "Maliyet"} grafiği: toplam ${total.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} ${unit}, ortalama ${average.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} ${unit}, tepe ${peak.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} ${unit}, ${data.length} veri noktası.`}
      >
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={data} margin={{ top: 8, right: 12, left: -8, bottom: 0 }}>
            <CartesianGrid stroke={gridColor} strokeDasharray="0" vertical={false} />
            <XAxis
              dataKey="time"
              tick={{ fontSize: 11, fill: mutedColor }}
              axisLine={{ stroke: gridColor }}
              tickLine={false}
              minTickGap={30}
            />
            <YAxis tick={{ fontSize: 11, fill: mutedColor }} axisLine={{ stroke: gridColor }} tickLine={false} width={48} />
            <Tooltip
              cursor={{ stroke: mutedColor, strokeWidth: 1 }}
              content={<CustomTooltip metric={metric} surfaceColor={surfaceColor} secondaryColor={secondaryColor} />}
            />
            <Line
              type="monotone"
              dataKey={metric === "energy" ? "energyKwh" : "cost"}
              stroke={color}
              strokeWidth={2}
              dot={(props) => {
                const point = props.payload as ChartPoint;
                if (!point.penalty) return <g key={props.key} />;
                return (
                  <circle
                    key={props.key}
                    cx={props.cx}
                    cy={props.cy}
                    r={3}
                    fill={dangerColor}
                    stroke={surfaceColor}
                    strokeWidth={1}
                  />
                );
              }}
              activeDot={{ r: 5, strokeWidth: 2, stroke: surfaceColor }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
