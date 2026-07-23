import { describe, expect, it } from "vitest";
import type { ApplianceCatalogItem } from "../types/applianceCatalog";
import { matchesApplianceCatalogSearch, normalizeForSearch } from "./applianceCatalogSearch";

const ITEM: ApplianceCatalogItem = {
  id: "cat-1",
  code: "COFFEE_MACHINE",
  displayName: "Kahve Makinesi",
  description: "Kısa süreli ve yüksek güçle çalışan kahve hazırlama cihazı.",
  category: "KITCHEN",
  behaviorProfile: "SHORT_HIGH_POWER",
  defaultSafePowerLimitWatt: 1500,
  defaultActiveMinWatt: 600,
  defaultActiveMaxWatt: 1300,
  defaultStandbyMinWatt: 0,
  defaultStandbyMaxWatt: 2,
  supportsStandby: true,
  supportsSchedule: false,
  supportsOperatingModes: false,
  iconKey: "coffee",
  featured: true,
  supportedTriggers: ["SAFE_POWER_LIMIT_BREACHED"],
};

describe("normalizeForSearch", () => {
  it("lower-cases using Turkish rules and trims", () => {
    expect(normalizeForSearch("  KAHVE Makinesi  ")).toBe("kahve makinesi");
    expect(normalizeForSearch("İZMİR")).toBe("izmir");
  });
});

describe("matchesApplianceCatalogSearch", () => {
  it("matches on displayName", () => {
    expect(matchesApplianceCatalogSearch(ITEM, "Mutfak", "kahve")).toBe(true);
  });

  it("matches on description", () => {
    expect(matchesApplianceCatalogSearch(ITEM, "Mutfak", "yüksek güç")).toBe(true);
  });

  it("matches on catalog code", () => {
    expect(matchesApplianceCatalogSearch(ITEM, "Mutfak", "coffee_machine")).toBe(true);
  });

  it("matches on the category label", () => {
    expect(matchesApplianceCatalogSearch(ITEM, "Mutfak", "mutfak")).toBe(true);
  });

  it("is case-insensitive with Turkish characters", () => {
    expect(matchesApplianceCatalogSearch(ITEM, "Mutfak", "KAHVE")).toBe(true);
  });

  it("returns false when nothing matches", () => {
    expect(matchesApplianceCatalogSearch(ITEM, "Mutfak", "klima")).toBe(false);
  });

  it("matches everything when the query is blank", () => {
    expect(matchesApplianceCatalogSearch(ITEM, "Mutfak", "   ")).toBe(true);
  });
});
