import { getApplianceCategoryLabel } from "../../../shared/constants/applianceCategoryLabels";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import { matchesApplianceCatalogSearch } from "../../../shared/utils/applianceCatalogSearch";

export const FEATURED_TAB = "FEATURED" as const;
export type ApplianceCatalogTab = typeof FEATURED_TAB | ApplianceCatalogItem["category"];

/** Combines the active category tab with the free-text search query — both narrow the result set
 * together (AND), matching how a typical filter panel behaves. */
export function filterCatalogItems(
  items: ApplianceCatalogItem[],
  options: { tab: ApplianceCatalogTab; query: string },
): ApplianceCatalogItem[] {
  const query = options.query.trim();
  if (query) {
    return items.filter((item) =>
      matchesApplianceCatalogSearch(item, getApplianceCategoryLabel(item.category), query),
    );
  }
  return items.filter((item) =>
    options.tab === FEATURED_TAB ? item.featured : item.category === options.tab,
  );
}
