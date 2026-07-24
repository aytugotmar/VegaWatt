import { describe, expect, it } from "vitest";
import { Coffee, Plug } from "lucide-react";
import { getApplianceDisplayName, getApplianceIcon } from "./applianceTypes";

describe("getApplianceDisplayName", () => {
  it("prefers the catalog display name when present", () => {
    expect(getApplianceDisplayName("REFRIGERATOR", "Buzdolabı Pro")).toBe("Buzdolabı Pro");
  });

  it("falls back to the type label when catalog display name is null", () => {
    expect(getApplianceDisplayName("REFRIGERATOR", null)).toBe("Buzdolabı");
  });

  it("falls back to the type label when catalog display name is undefined", () => {
    expect(getApplianceDisplayName("REFRIGERATOR")).toBe("Buzdolabı");
  });

  it("falls back to the unknown-device label when both are missing", () => {
    expect(getApplianceDisplayName(null, null)).toBe("Bilinmeyen cihaz");
  });
});

describe("getApplianceIcon", () => {
  it("resolves a catalog icon from the camelCase catalogIconKey, not the SCREAMING_SNAKE catalogCode", () => {
    // Regression test: catalogCode ("COFFEE_MACHINE") must never be passed here — only
    // catalogIconKey ("coffee") matches the icon map's keys.
    expect(getApplianceIcon("COFFEE_MACHINE", "coffee")).toBe(Coffee);
  });

  it("falls back to the generic icon when given the catalogCode shape instead of catalogIconKey", () => {
    expect(getApplianceIcon("COFFEE_MACHINE", "COFFEE_MACHINE")).toBe(Plug);
  });
});
