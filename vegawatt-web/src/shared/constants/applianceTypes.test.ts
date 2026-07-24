import { describe, expect, it } from "vitest";
import { getApplianceDisplayName } from "./applianceTypes";

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
