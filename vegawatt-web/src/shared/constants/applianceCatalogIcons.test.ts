import { Plug, Refrigerator } from "lucide-react";
import { describe, expect, it } from "vitest";
import { getApplianceCatalogIcon } from "./applianceCatalogIcons";

describe("getApplianceCatalogIcon", () => {
  it("resolves a known icon key", () => {
    expect(getApplianceCatalogIcon("refrigerator")).toBe(Refrigerator);
  });

  it("falls back to the generic icon for an unknown key", () => {
    expect(getApplianceCatalogIcon("some-future-appliance")).toBe(Plug);
  });

  it("falls back to the generic icon for a null/undefined key", () => {
    expect(getApplianceCatalogIcon(null)).toBe(Plug);
    expect(getApplianceCatalogIcon(undefined)).toBe(Plug);
  });
});
