import type { ApplianceCatalogItem } from "../types/applianceCatalog";

// Mirrors the backend's Locale.forLanguageTag("tr") lower-casing (ListApplianceCatalogQuery) so
// Turkish dotted/dotless I behave consistently between client-side and server-side search. Only
// for actual Turkish text (displayName/description/category label) — applying it to the ASCII
// catalog `code` (e.g. "COFFEE_MACHINE") would turn its "I" into a dotless "ı" under tr-TR rules,
// breaking a plain English-cased search for "machine".
export function normalizeForSearch(value: string): string {
  return value.toLocaleLowerCase("tr-TR").trim();
}

function normalizeCodeForSearch(value: string): string {
  return value.toLowerCase().trim();
}

export function matchesApplianceCatalogSearch(
  item: ApplianceCatalogItem,
  categoryLabel: string,
  query: string,
): boolean {
  const trimmedQuery = query.trim();
  if (!trimmedQuery) return true;
  const needle = normalizeForSearch(trimmedQuery);
  const codeNeedle = normalizeCodeForSearch(trimmedQuery);
  return (
    normalizeForSearch(item.displayName).includes(needle) ||
    normalizeForSearch(item.description).includes(needle) ||
    normalizeCodeForSearch(item.code).includes(codeNeedle) ||
    normalizeForSearch(categoryLabel).includes(needle)
  );
}
