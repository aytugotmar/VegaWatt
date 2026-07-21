import { LineChart as LineChartIcon } from "lucide-react";
import { useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { ConsumptionHistoryPoint } from "../../shared/types/home";
import { EmptyState } from "../../shared/components/EmptyState";
import { useTheme } from "../../shared/hooks/useTheme";
import { formatDateTime, toSafeNumber } from "../../shared/utils/format";

type MetricKey = "energy" | "cost";

const SERIES_COLOR: Record<MetricKey, { light: string; dark: string }> = {
  energy: { light: "#1D7A64", dark: "#68C2A1" },
  cost: { light: "#D09333", dark: "#E0B35B" },
};

const INK = {
  muted: { light: "#8a948d", dark: "#77837b" },
  grid: { light: "#dce2dc", dark: "#2e3932" },
  surface: { light: "#ffffff", dark: "#171d19" },
  secondary: { light: "#66716a", dark: "#a1aba4" },
  danger: { light: "#b84a3d", dark: "#e57d70" },
};

interface ChartPoint {
  time: string;
  energyKwh: number;
  cost: number;
  penalty: boolean;
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
        Birikimli enerji: {point.energyKwh.toLocaleString("tr-TR", { maximumFractionDigits: 3 })} kWh
      </p>
      <p className="tabular-nums" style={{ color: secondaryColor }}>
        Birikimli maliyet: {point.cost.toLocaleString("tr-TR", { maximumFractionDigits: 2 })} TRY
      </p>
      <p style={{ color: secondaryColor }}>{point.penalty ? "Tarife: Ceza" : "Tarife: Normal"}</p>
      {metric === "energy" ? null : null}
    </div>
  );
}

export function ConsumptionChart({ points }: { points: ConsumptionHistoryPoint[] }) {
  const { theme } = useTheme();
  const [metric, setMetric] = useState<MetricKey>("energy");

  if (points.length === 0) {
    return (
      <EmptyState
        icon={LineChartIcon}
        title="Geçmiş veri yok"
        description="Bu ev ve seçilen zaman aralığı için henüz tüketim geçmişi bulunmuyor."
      />
    );
  }

  const data: ChartPoint[] = points.map((point) => ({
    time: formatDateTime(point.snapshotTime),
    energyKwh: toSafeNumber(point.energyKwh),
    cost: toSafeNumber(point.cost),
    penalty: point.tariffState === "PENALTY",
  }));

  const color = SERIES_COLOR[metric][theme];
  const gridColor = INK.grid[theme];
  const mutedColor = INK.muted[theme];
  const surfaceColor = INK.surface[theme];
  const secondaryColor = INK.secondary[theme];
  const dangerColor = INK.danger[theme];

  return (
    <div className="flex flex-col gap-3" data-testid="consumption-chart">
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
  );
}
