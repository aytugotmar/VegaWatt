import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../test/testUtils";
import type { HomeLiveStatus, OperationalEvent, Recommendation } from "../../shared/types/home";
import { HomeDetailsDialog } from "./HomeDetailsDialog";

vi.mock("../../shared/api/homesApi", () => ({
  fetchLiveHomes: vi.fn(),
  fetchLiveHome: vi.fn(),
  fetchHomeHistory: vi.fn(),
  fetchHomeRecommendations: vi.fn(),
  fetchHomeEvents: vi.fn(),
  registerHome: vi.fn(),
}));

import { fetchHomeEvents, fetchHomeHistory, fetchHomeRecommendations, fetchLiveHome } from "../../shared/api/homesApi";

const HOME: HomeLiveStatus = {
  homeId: "home-1",
  homeName: "Bahçelievler",
  currentEnergyKwh: "12.5",
  currentCost: "250",
  energyQuotaPercentage: "40",
  budgetQuotaPercentage: "50",
  tariffState: "BASE",
  penaltyActive: false,
  lastUpdatedAt: new Date().toISOString(),
  appliances: [
    {
      applianceId: "app-1",
      applianceName: "Mutfak Buzdolabı",
      applianceType: "REFRIGERATOR",
      safePowerLimitWatt: "200",
      currentPowerWatt: "150",
      operatingState: "ACTIVE",
      operatingMode: "COMPRESSOR_ON",
      accumulatedEnergyKwh: "5",
      consecutiveBreachCount: 2,
      anomalous: true,
      standbyAnomalyActive: false,
      telemetryHealthStatus: "NORMAL",
      catalogCode: "REFRIGERATOR",
      catalogDisplayName: "Buzdolabı",
      catalogIconKey: "refrigerator",
      lastUpdatedAt: new Date().toISOString(),
    },
    {
      applianceId: "app-2",
      applianceName: "Özel Cihaz",
      applianceType: "OTHER",
      safePowerLimitWatt: null,
      currentPowerWatt: "80",
      operatingState: null,
      operatingMode: null,
      accumulatedEnergyKwh: "1",
      consecutiveBreachCount: 0,
      anomalous: false,
      standbyAnomalyActive: false,
      telemetryHealthStatus: "NORMAL",
      catalogCode: null,
      catalogDisplayName: null,
      catalogIconKey: null,
      lastUpdatedAt: new Date().toISOString(),
    },
  ],
};

const RECOMMENDATIONS: Recommendation[] = [
  {
    id: "rec-1",
    triggerType: "QUOTA_80",
    content: "Enerji kullanımınızı azaltın.",
    fallbackUsed: false,
    emailStatus: "PENDING",
    createdAt: new Date().toISOString(),
    triggerEventId: "event-1",
  },
];

const EVENTS: OperationalEvent[] = [
  {
    id: "event-1",
    applianceId: "app-1",
    eventType: "ENERGY_QUOTA_80_REACHED",
    eventTime: new Date().toISOString(),
    details: "Enerji kotasının %80'ine ulaşıldı.",
  },
];

describe("HomeDetailsDialog", () => {
  beforeEach(() => {
    vi.mocked(fetchLiveHome).mockReset().mockResolvedValue(HOME);
    vi.mocked(fetchHomeHistory).mockReset().mockResolvedValue([]);
    vi.mocked(fetchHomeRecommendations).mockReset().mockResolvedValue(RECOMMENDATIONS);
    vi.mocked(fetchHomeEvents).mockReset().mockResolvedValue(EVENTS);
  });

  it("shows the energy flow visual on the overview tab, branching to the top appliances", async () => {
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    expect(await screen.findByText("Enerji Akışı")).toBeInTheDocument();
    expect(screen.getByText("ŞEBEKE")).toBeInTheDocument();
    expect(screen.getByText("EV")).toBeInTheDocument();
    expect(screen.getAllByText("Mutfak Buzdolabı").length).toBeGreaterThan(0);
  });

  it("renders appliances with an anomaly badge, breach indicator, and null-safe limit", async () => {
    const user = userEvent.setup();
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    await user.click(await screen.findByRole("tab", { name: /Cihazlar/ }));

    expect((await screen.findAllByText("Mutfak Buzdolabı")).length).toBeGreaterThan(0);
    expect(screen.getAllByTestId("anomaly-badge").length).toBeGreaterThan(0);
    expect(screen.getAllByLabelText("Ardışık ihlal: 2/3").length).toBeGreaterThan(0);
    expect(screen.getAllByText("—").length).toBeGreaterThan(0);
  });

  it("shows the empty state when there is no consumption history", async () => {
    const user = userEvent.setup();
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    await user.click(await screen.findByRole("tab", { name: "Trendler" }));

    expect(await screen.findByText("Geçmiş veri yok")).toBeInTheDocument();
  });

  it("shows a pending email status on a recommendation", async () => {
    const user = userEvent.setup();
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    await user.click(await screen.findByRole("tab", { name: /Öneriler/ }));

    expect(await screen.findByText("E-posta gönderiliyor")).toBeInTheDocument();
  });

  it("shows the linked event's historical detail under a recommendation, not today's live numbers", async () => {
    const user = userEvent.setup();
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    await user.click(await screen.findByRole("tab", { name: /Öneriler/ }));

    expect(await screen.findByText(/Enerji kotasının %80'ine ulaşıldı\./)).toBeInTheDocument();
  });

  it("shows the event timeline on the events tab, and an empty state when there are none", async () => {
    const user = userEvent.setup();
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    await user.click(await screen.findByRole("tab", { name: /Olaylar/ }));

    expect(await screen.findByText("Enerji kotası %80'e ulaştı")).toBeInTheDocument();
    expect(screen.getByText("Mutfak Buzdolabı", { exact: false })).toBeInTheDocument();
  });

  it("shows an empty state on the events tab when there are no events", async () => {
    vi.mocked(fetchHomeEvents).mockReset().mockResolvedValue([]);
    const user = userEvent.setup();
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    await user.click(await screen.findByRole("tab", { name: /Olaylar/ }));

    expect(await screen.findByText("Henüz olay yok")).toBeInTheDocument();
  });

  it("shows an anomaly count badge on the devices tab, and recommendation/event counts on their tabs", async () => {
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={vi.fn()} />);

    const devicesTab = await screen.findByRole("tab", { name: /Cihazlar/ });
    const insightsTab = await screen.findByRole("tab", { name: /Öneriler/ });
    const eventsTab = await screen.findByRole("tab", { name: /Olaylar/ });

    expect(devicesTab).toHaveTextContent("1");
    expect(insightsTab).toHaveTextContent("1");
    expect(eventsTab).toHaveTextContent("1");
  });

  it("closes when Escape is pressed", async () => {
    const onClose = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<HomeDetailsDialog homeId="home-1" onClose={onClose} />);

    await screen.findByText("Bahçelievler");
    await user.keyboard("{Escape}");

    expect(onClose).toHaveBeenCalled();
  });
});
