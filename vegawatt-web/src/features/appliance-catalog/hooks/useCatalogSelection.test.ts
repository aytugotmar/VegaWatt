import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import { useCatalogSelection } from "./useCatalogSelection";

const REFRIGERATOR: ApplianceCatalogItem = {
  id: "cat-refrigerator",
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
};

describe("useCatalogSelection", () => {
  it("adds an instance snapshotting the catalog item's defaults", () => {
    const { result } = renderHook(() => useCatalogSelection());

    act(() => result.current.addInstance(REFRIGERATOR));

    expect(result.current.selectedAppliances).toHaveLength(1);
    const instance = result.current.selectedAppliances[0];
    expect(instance.catalogItemId).toBe("cat-refrigerator");
    expect(instance.name).toBe("Buzdolabı");
    expect(instance.safePowerLimitWatt).toBe("220");
    expect(instance.overridden).toEqual({
      safePowerLimitWatt: false,
      simulationMinWatt: false,
      simulationMaxWatt: false,
    });
  });

  it("names subsequent instances of the same catalog item with an ordinal suffix", () => {
    const { result } = renderHook(() => useCatalogSelection());

    act(() => {
      result.current.addInstance(REFRIGERATOR);
      result.current.addInstance(REFRIGERATOR);
    });

    expect(result.current.selectedAppliances.map((instance) => instance.name)).toEqual(["Buzdolabı", "Buzdolabı 2"]);
  });

  it("decrementQuantity removes the most recently added instance of that catalog item", () => {
    const { result } = renderHook(() => useCatalogSelection());

    act(() => {
      result.current.addInstance(REFRIGERATOR);
      result.current.addInstance(REFRIGERATOR);
    });
    act(() => result.current.decrementQuantity("cat-refrigerator"));

    expect(result.current.selectedAppliances).toHaveLength(1);
    expect(result.current.selectedAppliances[0].name).toBe("Buzdolabı");
  });

  it("removeInstance removes only the targeted instance, leaving siblings untouched", () => {
    const { result } = renderHook(() => useCatalogSelection());

    act(() => {
      result.current.addInstance(REFRIGERATOR);
      result.current.addInstance(REFRIGERATOR);
    });
    const [first, second] = result.current.selectedAppliances;

    act(() => result.current.removeInstance(first.instanceKey));

    expect(result.current.selectedAppliances).toHaveLength(1);
    expect(result.current.selectedAppliances[0].instanceKey).toBe(second.instanceKey);
  });

  it("renameInstance only affects the targeted instance", () => {
    const { result } = renderHook(() => useCatalogSelection());

    act(() => {
      result.current.addInstance(REFRIGERATOR);
      result.current.addInstance(REFRIGERATOR);
    });
    const [first, second] = result.current.selectedAppliances;

    act(() => result.current.renameInstance(first.instanceKey, "Mutfak Buzdolabı"));

    expect(result.current.selectedAppliances.find((i) => i.instanceKey === first.instanceKey)?.name).toBe(
      "Mutfak Buzdolabı",
    );
    expect(result.current.selectedAppliances.find((i) => i.instanceKey === second.instanceKey)?.name).toBe(
      "Buzdolabı 2",
    );
  });

  it("updateOverride marks the field as overridden and does not affect other instances", () => {
    const { result } = renderHook(() => useCatalogSelection());

    act(() => {
      result.current.addInstance(REFRIGERATOR);
      result.current.addInstance(REFRIGERATOR);
    });
    const [first, second] = result.current.selectedAppliances;

    act(() => result.current.updateOverride(first.instanceKey, "safePowerLimitWatt", "250"));

    const updatedFirst = result.current.selectedAppliances.find((i) => i.instanceKey === first.instanceKey)!;
    const untouchedSecond = result.current.selectedAppliances.find((i) => i.instanceKey === second.instanceKey)!;
    expect(updatedFirst.safePowerLimitWatt).toBe("250");
    expect(updatedFirst.overridden.safePowerLimitWatt).toBe(true);
    expect(untouchedSecond.overridden.safePowerLimitWatt).toBe(false);
  });

  it("never surfaces an error for an untouched catalog default", () => {
    const { result } = renderHook(() => useCatalogSelection());
    act(() => result.current.addInstance(REFRIGERATOR));

    expect(result.current.errors).toEqual({});
  });

  it("surfaces an error only for the field the user actually overrode", () => {
    const { result } = renderHook(() => useCatalogSelection());
    act(() => result.current.addInstance(REFRIGERATOR));
    const instanceKey = result.current.selectedAppliances[0].instanceKey;

    // Below the untouched default min of 80.
    act(() => result.current.updateOverride(instanceKey, "simulationMaxWatt", "10"));

    expect(result.current.errors[instanceKey]?.simulationMaxWatt).toBeDefined();
    expect(result.current.errors[instanceKey]?.simulationMinWatt).toBeUndefined();
    expect(result.current.errors[instanceKey]?.safePowerLimitWatt).toBeUndefined();
  });

  it("requires a non-empty name for every instance", () => {
    const { result } = renderHook(() => useCatalogSelection());
    act(() => result.current.addInstance(REFRIGERATOR));
    const instanceKey = result.current.selectedAppliances[0].instanceKey;

    act(() => result.current.renameInstance(instanceKey, "   "));

    expect(result.current.errors[instanceKey]?.name).toBeDefined();
  });
});
