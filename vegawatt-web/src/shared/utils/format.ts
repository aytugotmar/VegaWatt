import type { Language } from "../i18n/LanguageContext";

export function toSafeNumber(value: string | number | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === "string" ? Number(value) : value;
  return Number.isFinite(parsed) ? parsed : 0;
}

function localeOf(lang?: Language): string {
  return lang === "en" ? "en-US" : "tr-TR";
}

export function formatCurrency(value: string | number | null | undefined, lang?: Language): string {
  const amount = toSafeNumber(value);
  const formatted = amount.toLocaleString(localeOf(lang), { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return `${formatted} TRY`;
}

export function formatPercentage(value: string | number | null | undefined, lang?: Language): string {
  const amount = toSafeNumber(value);
  const formatted = amount.toLocaleString(localeOf(lang), { minimumFractionDigits: 0, maximumFractionDigits: 1 });
  return lang === "en" ? `${formatted}%` : `%${formatted}`;
}

export function formatEnergy(value: string | number | null | undefined, lang?: Language): string {
  const amount = toSafeNumber(value);
  const formatted = amount.toLocaleString(localeOf(lang), { minimumFractionDigits: 2, maximumFractionDigits: 4 });
  return `${formatted} kWh`;
}

export function formatPower(value: string | number | null | undefined, lang?: Language): string {
  const amount = toSafeNumber(value);
  const loc = localeOf(lang);
  if (amount >= 1000) {
    return `${(amount / 1000).toLocaleString(loc, { minimumFractionDigits: 1, maximumFractionDigits: 2 })} kW`;
  }
  return `${amount.toLocaleString(loc, { minimumFractionDigits: 0, maximumFractionDigits: 1 })} W`;
}

export function formatDateTime(value: string | null | undefined, lang?: Language): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString(localeOf(lang));
}

const RELATIVE_TIME_FORMAT_TR = new Intl.RelativeTimeFormat("tr-TR", { numeric: "auto" });
const RELATIVE_TIME_FORMAT_EN = new Intl.RelativeTimeFormat("en-US", { numeric: "auto" });

export function formatRelativeTime(value: string | null | undefined, lang?: Language): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";

  const diffSeconds = Math.round((date.getTime() - Date.now()) / 1000);
  const absSeconds = Math.abs(diffSeconds);
  const formatter = lang === "en" ? RELATIVE_TIME_FORMAT_EN : RELATIVE_TIME_FORMAT_TR;

  if (absSeconds < 45) return lang === "en" ? "just now" : "az önce";
  if (absSeconds < 3600) return formatter.format(Math.round(diffSeconds / 60), "minute");
  if (absSeconds < 86_400) return formatter.format(Math.round(diffSeconds / 3600), "hour");
  if (absSeconds < 2_592_000) return formatter.format(Math.round(diffSeconds / 86_400), "day");
  return formatDateTime(value, lang);
}
