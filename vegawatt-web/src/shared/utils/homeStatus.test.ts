import { describe, expect, it } from "vitest";
import { getApplianceHealthTone, getHomeHealthStatus, getQuotaTone } from "./homeStatus";

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

function appliance(overrides: Partial<Parameters<typeof getApplianceHealthTone>[0]> = {}) {
  return {
    telemetryHealthStatus: "NORMAL" as const,
    anomalous: false,
    standbyAnomalyActive: false,
    ...overrides,
  };
}

describe("getApplianceHealthTone", () => {
  it("returns offline when telemetry is OFFLINE regardless of other flags", () => {
    expect(
      getApplianceHealthTone(appliance({ telemetryHealthStatus: "OFFLINE", anomalous: true })),
    ).toBe("offline");
  });

  it("returns stale when telemetry is STALE and not offline", () => {
    expect(getApplianceHealthTone(appliance({ telemetryHealthStatus: "STALE", anomalous: true }))).toBe("stale");
  });

  it("returns anomalous when anomalous and telemetry is normal", () => {
    expect(getApplianceHealthTone(appliance({ anomalous: true, standbyAnomalyActive: true }))).toBe("anomalous");
  });

  it("returns warning when only standbyAnomalyActive is set", () => {
    expect(getApplianceHealthTone(appliance({ standbyAnomalyActive: true }))).toBe("warning");
  });

  it("returns normal otherwise", () => {
    expect(getApplianceHealthTone(appliance())).toBe("normal");
  });
});
