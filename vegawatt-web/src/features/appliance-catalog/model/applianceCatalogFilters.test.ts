import { describe, expect, it } from "vitest";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import { FEATURED_TAB, filterCatalogItems } from "./applianceCatalogFilters";

function makeItem(overrides: Partial<ApplianceCatalogItem>): ApplianceCatalogItem {
  return {
    id: "cat-1",
    code: "REFRIGERATOR",
    displayName: "Buzdolabı",
    description: "Evin ana soğutucusu.",
    category: "KITCHEN",
    behaviorProfile: "THERMOSTATIC_CYCLE",
    defaultSafePowerLimitWatt: 220,
    defaultActiveMinWatt: 80,
    defaultActiveMaxWatt: 180,
    defaultStandbyMinWatt: 2,
    defaultStandbyMaxWatt: 15,
    supportsStandby: true,
    supportsSchedule: false,
    supportsOperatingModes: true,
    iconKey: "refrigerator",
    featured: true,
    supportedTriggers: [],
    ...overrides,
  };
}

const REFRIGERATOR = makeItem({ id: "cat-fridge", code: "REFRIGERATOR", displayName: "Buzdolabı", featured: true, category: "KITCHEN" });
const AIR_CONDITIONER = makeItem({
  id: "cat-ac",
  code: "AIR_CONDITIONER",
  displayName: "Klima",
  featured: false,
  category: "CLIMATE",
});
const ITEMS = [REFRIGERATOR, AIR_CONDITIONER];

describe("filterCatalogItems", () => {
  it("shows only featured items on the Featured tab", () => {
    const result = filterCatalogItems(ITEMS, { tab: FEATURED_TAB, query: "" });
    expect(result).toEqual([REFRIGERATOR]);
  });

  it("shows only items in the active category tab", () => {
    const result = filterCatalogItems(ITEMS, { tab: "CLIMATE", query: "" });
    expect(result).toEqual([AIR_CONDITIONER]);
  });

  it("combines category and search with AND", () => {
    const result = filterCatalogItems(ITEMS, { tab: "KITCHEN", query: "klima" });
    expect(result).toEqual([]);
  });

  it("matches an item that is in the tab and matches the search", () => {
    const result = filterCatalogItems(ITEMS, { tab: "KITCHEN", query: "buzdolabı" });
    expect(result).toEqual([REFRIGERATOR]);
  });

  it("searches by ASCII code even when the query is lowercase 'i'", () => {
    const coffeeMachine = makeItem({ id: "cat-coffee", code: "COFFEE_MACHINE", displayName: "Kahve Makinesi", featured: false, category: "KITCHEN" });
    const result = filterCatalogItems([coffeeMachine], { tab: "KITCHEN", query: "machine" });
    expect(result).toEqual([coffeeMachine]);
  });
});
