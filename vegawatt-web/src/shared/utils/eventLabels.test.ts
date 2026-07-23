import { describe, expect, it } from "vitest";
import { getEventTone, getEventTypeLabel } from "./eventLabels";

describe("getEventTypeLabel", () => {
  it("returns a Turkish label for a known event type", () => {
    expect(getEventTypeLabel("APPLIANCE_ANOMALY_DETECTED")).toBe("Cihaz anomalisi tespit edildi");
  });

  it("falls back to a generic label for an unknown event type", () => {
    expect(getEventTypeLabel("SOMETHING_NEW")).toBe("Sistem olayı");
  });
});

describe("getEventTone", () => {
  it("marks anomaly detection as danger", () => {
    expect(getEventTone("APPLIANCE_ANOMALY_DETECTED")).toBe("danger");
  });

  it("marks recovery as success", () => {
    expect(getEventTone("APPLIANCE_ANOMALY_RECOVERED")).toBe("success");
  });

  it("marks a stale telemetry warning distinctly from an offline danger", () => {
    expect(getEventTone("APPLIANCE_TELEMETRY_STALE")).toBe("warning");
    expect(getEventTone("APPLIANCE_TELEMETRY_OFFLINE")).toBe("danger");
  });

  it("falls back to info for an unknown event type", () => {
    expect(getEventTone("SOMETHING_NEW")).toBe("info");
  });
});
