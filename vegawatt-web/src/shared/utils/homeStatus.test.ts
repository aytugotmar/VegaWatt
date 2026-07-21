import { describe, expect, it } from "vitest";
import { getHomeHealthStatus, getQuotaTone } from "./homeStatus";

function home(overrides: Partial<Parameters<typeof getHomeHealthStatus>[0]> = {}) {
  return {
    penaltyActive: false,
    tariffState: "BASE" as const,
    energyQuotaPercentage: "10",
    budgetQuotaPercentage: "10",
    ...overrides,
  };
}

describe("getHomeHealthStatus", () => {
  it("returns PENALTY when penaltyActive is true", () => {
    expect(getHomeHealthStatus(home({ penaltyActive: true }))).toBe("PENALTY");
  });

  it("returns PENALTY when tariffState is PENALTY even without penaltyActive", () => {
    expect(getHomeHealthStatus(home({ tariffState: "PENALTY" }))).toBe("PENALTY");
  });

  it("returns CRITICAL when either quota reaches 100%", () => {
    expect(getHomeHealthStatus(home({ energyQuotaPercentage: "100" }))).toBe("CRITICAL");
    expect(getHomeHealthStatus(home({ budgetQuotaPercentage: "150" }))).toBe("CRITICAL");
  });

  it("returns WARNING when either quota reaches 80% but under 100%", () => {
    expect(getHomeHealthStatus(home({ energyQuotaPercentage: "85" }))).toBe("WARNING");
  });

  it("returns NORMAL otherwise", () => {
    expect(getHomeHealthStatus(home())).toBe("NORMAL");
  });
});

describe("getQuotaTone", () => {
  it("classifies critical/warning/normal thresholds", () => {
    expect(getQuotaTone(105)).toBe("critical");
    expect(getQuotaTone(80)).toBe("warning");
    expect(getQuotaTone(50)).toBe("normal");
    expect(getQuotaTone(null)).toBe("normal");
  });
});
