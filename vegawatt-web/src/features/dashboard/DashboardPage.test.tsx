import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../test/testUtils";
import type { HomeLiveSummary } from "../../shared/types/home";
import { DashboardPage } from "./DashboardPage";

vi.mock("../../shared/api/homesApi", () => ({
  fetchLiveHomes: vi.fn(),
  fetchLiveHome: vi.fn(),
  fetchHomeHistory: vi.fn(),
  fetchHomeRecommendations: vi.fn(),
  registerHome: vi.fn(),
}));

import { fetchHomeHistory, fetchHomeRecommendations, fetchLiveHome, fetchLiveHomes } from "../../shared/api/homesApi";

function makeHome(overrides: Partial<HomeLiveSummary> = {}): HomeLiveSummary {
  return {
    homeId: "home-1",
    homeName: "Ev 1",
    currentEnergyKwh: "10",
    currentCost: "100",
    energyQuotaPercentage: "10",
    budgetQuotaPercentage: "10",
    tariffState: "BASE",
    penaltyActive: false,
    lastUpdatedAt: new Date().toISOString(),
    ...overrides,
  };
}

describe("DashboardPage", () => {
  beforeEach(() => {
    vi.mocked(fetchLiveHomes).mockReset();
    vi.mocked(fetchLiveHome).mockReset().mockResolvedValue({
      ...makeHome(),
      appliances: [],
    });
    vi.mocked(fetchHomeHistory).mockReset().mockResolvedValue([]);
    vi.mocked(fetchHomeRecommendations).mockReset().mockResolvedValue([]);
  });

  it("shows the empty state when there are no homes", async () => {
    vi.mocked(fetchLiveHomes).mockResolvedValue([]);
    renderWithProviders(<DashboardPage />);

    expect(await screen.findByText("Henüz kayıtlı ev yok")).toBeInTheDocument();
  });

  it("renders homes in the table once loaded", async () => {
    vi.mocked(fetchLiveHomes).mockResolvedValue([makeHome({ homeName: "Bahçelievler" })]);
    renderWithProviders(<DashboardPage />);

    expect((await screen.findAllByText("Bahçelievler")).length).toBeGreaterThan(0);
  });

  it("labels a penalty-tariff home as such and filters search results down to a no-match empty state", async () => {
    vi.mocked(fetchLiveHomes).mockResolvedValue([
      makeHome({ homeId: "home-1", homeName: "Normal Ev", tariffState: "BASE" }),
      makeHome({ homeId: "home-2", homeName: "Ceza Evi", tariffState: "PENALTY", penaltyActive: true }),
    ]);
    const user = userEvent.setup();
    renderWithProviders(<DashboardPage />);

    await screen.findAllByText("Normal Ev");
    const penaltyRow = screen.getByRole("button", { name: "Ceza Evi detaylarını görüntüle" });
    expect(within(penaltyRow).getByText("Yüksek tarife")).toBeInTheDocument();

    const searchInput = screen.getByLabelText("Ev ara");
    await user.type(searchInput, "olmayan-ev");

    expect(await screen.findByText("Sonuç bulunamadı")).toBeInTheDocument();
  });

  it("opens the home details dialog when a row is clicked", async () => {
    vi.mocked(fetchLiveHomes).mockResolvedValue([makeHome({ homeName: "Bahçelievler" })]);
    const user = userEvent.setup();
    renderWithProviders(<DashboardPage />);

    const row = await screen.findByRole("button", { name: "Bahçelievler detaylarını görüntüle" });
    await user.click(row);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });
  });

  it("renders homes as cards in USER mode and navigates to the home's own page on click", async () => {
    vi.mocked(fetchLiveHomes).mockResolvedValue([makeHome({ homeName: "Bahçelievler" })]);
    const user = userEvent.setup();
    renderWithProviders(<DashboardPage mode="USER" />);

    const card = await screen.findByRole("button", { name: "Bahçelievler detaylarını görüntüle" });
    expect(card).toHaveAttribute("data-testid", "home-card");

    await user.click(card);

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });
});
