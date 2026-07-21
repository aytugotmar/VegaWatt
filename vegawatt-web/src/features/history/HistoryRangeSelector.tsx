export type HistoryRangeKey = "1H" | "24H" | "7D" | "30D";

const RANGE_HOURS: Record<HistoryRangeKey, number> = {
  "1H": 1,
  "24H": 24,
  "7D": 24 * 7,
  "30D": 24 * 30,
};

const RANGE_LABELS: Record<HistoryRangeKey, string> = {
  "1H": "Son 1 saat",
  "24H": "Son 24 saat",
  "7D": "Son 7 gün",
  "30D": "Son 30 gün",
};

const RANGE_ORDER: HistoryRangeKey[] = ["1H", "24H", "7D", "30D"];

export function computeHistoryRange(rangeKey: HistoryRangeKey, now: Date = new Date()): { from: string; to: string } {
  const to = now.toISOString();
  const from = new Date(now.getTime() - RANGE_HOURS[rangeKey] * 60 * 60 * 1000).toISOString();
  return { from, to };
}

interface HistoryRangeSelectorProps {
  value: HistoryRangeKey;
  onChange: (value: HistoryRangeKey) => void;
}

export function HistoryRangeSelector({ value, onChange }: HistoryRangeSelectorProps) {
  return (
    <div className="inline-flex rounded-input border border-border bg-surface p-0.5" role="group" aria-label="Zaman aralığı">
      {RANGE_ORDER.map((key) => (
        <button
          key={key}
          type="button"
          onClick={() => onChange(key)}
          aria-pressed={value === key}
          className={`rounded-[6px] px-2.5 py-1 text-xs font-medium transition ${
            value === key ? "bg-primary text-white" : "text-text-secondary hover:bg-surface-subtle"
          }`}
        >
          {RANGE_LABELS[key]}
        </button>
      ))}
    </div>
  );
}
