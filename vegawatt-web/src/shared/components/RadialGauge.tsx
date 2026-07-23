interface RadialGaugeProps {
  percentage: number;
  size?: number;
  strokeWidth?: number;
}

export function RadialGauge({ percentage, size = 88, strokeWidth = 7 }: RadialGaugeProps) {
  const clamped = Math.max(0, Math.min(100, percentage));
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference * (1 - clamped / 100);
  const center = size / 2;

  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90" role="img" aria-label={`%${Math.round(percentage)}`}>
        <circle cx={center} cy={center} r={radius} fill="none" stroke="var(--color-surface-subtle)" strokeWidth={strokeWidth} />
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="var(--color-primary)"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="glow-stroke transition-[stroke-dashoffset] duration-500"
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <span className="tabular-nums text-lg font-semibold text-text-primary">%{Math.round(clamped)}</span>
      </div>
    </div>
  );
}
