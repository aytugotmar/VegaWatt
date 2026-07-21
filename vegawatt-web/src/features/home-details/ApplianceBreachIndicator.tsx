const BREACH_THRESHOLD = 3;

interface ApplianceBreachIndicatorProps {
  consecutiveBreachCount: number;
}

export function ApplianceBreachIndicator({ consecutiveBreachCount }: ApplianceBreachIndicatorProps) {
  const filled = Math.min(consecutiveBreachCount, BREACH_THRESHOLD);

  return (
    <div className="flex items-center gap-1.5" aria-label={`Ardışık ihlal: ${filled}/${BREACH_THRESHOLD}`}>
      <div className="flex items-center gap-0.5" aria-hidden="true">
        {Array.from({ length: BREACH_THRESHOLD }).map((_, index) => (
          <span
            key={index}
            className={`h-1.5 w-1.5 rounded-full ${index < filled ? "bg-danger" : "bg-surface-subtle"}`}
          />
        ))}
      </div>
      <span className="tabular-nums text-xs text-text-secondary">
        {filled}/{BREACH_THRESHOLD}
      </span>
    </div>
  );
}
