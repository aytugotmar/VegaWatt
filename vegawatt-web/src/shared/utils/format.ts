export function toSafeNumber(value: string | number | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === "string" ? Number(value) : value;
  return Number.isFinite(parsed) ? parsed : 0;
}

export function formatCurrency(value: string | number | null | undefined): string {
  const amount = toSafeNumber(value);
  return `${amount.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} TRY`;
}

export function formatPercentage(value: string | number | null | undefined): string {
  const amount = toSafeNumber(value);
  return `%${amount.toLocaleString("tr-TR", { minimumFractionDigits: 0, maximumFractionDigits: 1 })}`;
}

export function formatEnergy(value: string | number | null | undefined): string {
  const amount = toSafeNumber(value);
  return `${amount.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 4 })} kWh`;
}

export function formatPower(value: string | number | null | undefined): string {
  const amount = toSafeNumber(value);
  return `${amount.toLocaleString("tr-TR", { minimumFractionDigits: 0, maximumFractionDigits: 1 })} W`;
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("tr-TR");
}

const RELATIVE_TIME_FORMAT = new Intl.RelativeTimeFormat("tr-TR", { numeric: "auto" });

export function formatRelativeTime(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";

  const diffSeconds = Math.round((date.getTime() - Date.now()) / 1000);
  const absSeconds = Math.abs(diffSeconds);

  if (absSeconds < 45) return "az önce";
  if (absSeconds < 3600) return RELATIVE_TIME_FORMAT.format(Math.round(diffSeconds / 60), "minute");
  if (absSeconds < 86_400) return RELATIVE_TIME_FORMAT.format(Math.round(diffSeconds / 3600), "hour");
  if (absSeconds < 2_592_000) return RELATIVE_TIME_FORMAT.format(Math.round(diffSeconds / 86_400), "day");
  return formatDateTime(value);
}
