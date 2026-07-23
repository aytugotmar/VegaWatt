import { describe, expect, it } from "vitest";
import { validateApplianceLimits } from "./registrationSchema";

describe("validateApplianceLimits", () => {
  const VALID = { safePowerLimitWatt: "200", simulationMinWatt: "50", simulationMaxWatt: "150" };

  it("accepts a valid range", () => {
    expect(validateApplianceLimits(VALID)).toEqual({});
  });

  it("requires a positive safe power limit", () => {
    expect(validateApplianceLimits({ ...VALID, safePowerLimitWatt: "0" }).safePowerLimitWatt).toBeDefined();
  });

  it("requires a non-negative minimum", () => {
    expect(validateApplianceLimits({ ...VALID, simulationMinWatt: "-1" }).simulationMinWatt).toBeDefined();
  });

  it("requires the maximum to be greater than the minimum", () => {
    expect(validateApplianceLimits({ ...VALID, simulationMaxWatt: "50" }).simulationMaxWatt).toBeDefined();
  });

  it("requires the maximum to not exceed the safe power limit", () => {
    const errors = validateApplianceLimits({
      safePowerLimitWatt: "100",
      simulationMinWatt: "10",
      simulationMaxWatt: "150",
    });
    expect(errors.simulationMaxWatt).toBe("Maksimum değer, güvenli limiti aşamaz.");
  });

  it("allows the maximum to equal the safe power limit", () => {
    const errors = validateApplianceLimits({
      safePowerLimitWatt: "100",
      simulationMinWatt: "10",
      simulationMaxWatt: "100",
    });
    expect(errors.simulationMaxWatt).toBeUndefined();
  });
});
