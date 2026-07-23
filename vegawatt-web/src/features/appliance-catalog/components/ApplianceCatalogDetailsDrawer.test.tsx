import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import { ApplianceCatalogDetailsDrawer } from "./ApplianceCatalogDetailsDrawer";

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
  supportedTriggers: ["SAFE_POWER_LIMIT_BREACHED", "UNUSUAL_STANDBY_CONSUMPTION"],
};

describe("ApplianceCatalogDetailsDrawer", () => {
  it("renders the trigger checklist with a Turkish label (icon + text) instead of the raw enum", () => {
    render(<ApplianceCatalogDetailsDrawer item={REFRIGERATOR} onClose={vi.fn()} onAdd={vi.fn()} />);

    expect(screen.getByText("Aşırı güç")).toBeInTheDocument();
    expect(screen.getByText("Yüksek bekleme tüketimi")).toBeInTheDocument();
    expect(screen.queryByText("SAFE_POWER_LIMIT_BREACHED")).not.toBeInTheDocument();
    expect(screen.queryByText("UNUSUAL_STANDBY_CONSUMPTION")).not.toBeInTheDocument();
  });

  it("shows the behavior profile as a friendly label, not the raw enum", () => {
    render(<ApplianceCatalogDetailsDrawer item={REFRIGERATOR} onClose={vi.fn()} onAdd={vi.fn()} />);

    expect(screen.getByText("Termostat kontrollü")).toBeInTheDocument();
    expect(screen.queryByText("THERMOSTATIC_CYCLE")).not.toBeInTheDocument();
  });

  it("calls onAdd when the CTA is clicked", async () => {
    const onAdd = vi.fn();
    render(<ApplianceCatalogDetailsDrawer item={REFRIGERATOR} onClose={vi.fn()} onAdd={onAdd} />);

    screen.getByRole("button", { name: "Bu cihazı ekle" }).click();

    expect(onAdd).toHaveBeenCalledTimes(1);
  });
});
