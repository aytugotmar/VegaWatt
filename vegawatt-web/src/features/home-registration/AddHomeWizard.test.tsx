import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../test/testUtils";
import { ApiError } from "../../shared/api/client";
import type { ApplianceCatalogItem } from "../../shared/types/applianceCatalog";
import { AddHomeWizard } from "./AddHomeWizard";

vi.mock("../../shared/api/homesApi", () => ({
  fetchLiveHomes: vi.fn(),
  fetchLiveHome: vi.fn(),
  fetchHomeHistory: vi.fn(),
  fetchHomeRecommendations: vi.fn(),
  registerHome: vi.fn(),
}));

vi.mock("../../shared/api/applianceCatalogApi", () => ({
  fetchApplianceCatalog: vi.fn(),
}));

import { registerHome } from "../../shared/api/homesApi";
import { fetchApplianceCatalog } from "../../shared/api/applianceCatalogApi";

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
  supportedTriggers: ["SAFE_POWER_LIMIT_BREACHED"],
};

const AIR_CONDITIONER: ApplianceCatalogItem = {
  id: "cat-air-conditioner",
  code: "AIR_CONDITIONER",
  displayName: "Klima",
  description: "Soğutma ve ısıtma yapan iklimlendirme cihazı.",
  category: "CLIMATE",
  behaviorProfile: "THERMOSTATIC_CYCLE",
  defaultSafePowerLimitWatt: 2500,
  defaultActiveMinWatt: 500,
  defaultActiveMaxWatt: 2200,
  defaultStandbyMinWatt: 1,
  defaultStandbyMaxWatt: 5,
  supportsStandby: true,
  supportsSchedule: false,
  supportsOperatingModes: true,
  iconKey: "airConditioner",
  featured: false,
  supportedTriggers: ["SAFE_POWER_LIMIT_BREACHED"],
};

const CATALOG_FIXTURE: ApplianceCatalogItem[] = [REFRIGERATOR, AIR_CONDITIONER];

async function fillHomeInfoStep(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Ev adı"), "Bahçelievler Dairesi");
  await user.type(screen.getByLabelText("Bildirim e-postası"), "test@example.com");
  await user.click(screen.getByRole("button", { name: "İleri" }));
}

async function goToAppliancesStep(user: ReturnType<typeof userEvent.setup>) {
  await fillHomeInfoStep(user);
  await user.click(screen.getByRole("button", { name: "İleri" }));
  await screen.findByText("Buzdolabı");
}

function addRefrigerator(user: ReturnType<typeof userEvent.setup>) {
  return user.click(screen.getByRole("button", { name: "Buzdolabı ekle" }));
}

describe("AddHomeWizard", () => {
  beforeEach(() => {
    vi.mocked(registerHome).mockReset();
    vi.mocked(fetchApplianceCatalog).mockReset();
    vi.mocked(fetchApplianceCatalog).mockResolvedValue(CATALOG_FIXTURE);
  });

  it("blocks advancing from step 1 until required fields are filled", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "İleri" }));
    expect(await screen.findByText("Ev adı zorunludur.")).toBeInTheDocument();
    expect(screen.getByLabelText("Ev adı")).toBeInTheDocument();
  });

  it("rejects a penalty tariff lower than the base tariff on step 2", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);

    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "Gelişmiş tarife ayarları" }));

    const penaltyInput = screen.getByLabelText("Ceza tarifesi (TRY/kWh)");
    await user.clear(penaltyInput);
    await user.type(penaltyInput, "1");
    await user.click(screen.getByRole("button", { name: "İleri" }));

    expect(await screen.findByText("Ceza tarifesi, baz tarifeden düşük olamaz.")).toBeInTheDocument();
  });

  it("requires at least one appliance before reaching the review step", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);

    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "İleri" }));

    await user.click(screen.getByRole("button", { name: "İleri" }));
    expect(await screen.findByText("En az bir cihaz seçmeli veya eklemelisiniz.")).toBeInTheDocument();
  });

  it("shows a loading state while the catalog is fetching, then renders the catalog items", async () => {
    let resolveCatalog: (value: ApplianceCatalogItem[]) => void = () => {};
    vi.mocked(fetchApplianceCatalog).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCatalog = resolve;
        }),
    );

    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "İleri" }));

    expect(screen.getByText("Cihaz kataloğu yükleniyor…")).toBeInTheDocument();

    resolveCatalog(CATALOG_FIXTURE);
    expect(await screen.findByText("Buzdolabı")).toBeInTheDocument();
  });

  it("shows an error state with a retry action when the catalog fails to load, and never falls back to a hardcoded list", async () => {
    vi.mocked(fetchApplianceCatalog).mockRejectedValueOnce(new ApiError("Sunucu hatası."));

    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "İleri" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Cihaz kataloğu yüklenemedi.");
    expect(screen.queryByText("Buzdolabı")).not.toBeInTheDocument();

    vi.mocked(fetchApplianceCatalog).mockResolvedValueOnce(CATALOG_FIXTURE);
    await user.click(screen.getByRole("button", { name: "Yeniden Dene" }));

    expect(await screen.findByText("Buzdolabı")).toBeInTheDocument();
  });

  it("supports switching category tabs and combines a tab with search using AND", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    // The Featured tab is active by default — only the featured Buzdolabı is visible.
    expect(screen.getByText("Buzdolabı")).toBeInTheDocument();
    expect(screen.queryByText("Klima")).not.toBeInTheDocument();

    await user.click(screen.getByRole("tab", { name: "Isıtma ve İklimlendirme" }));
    expect(await screen.findByText("Klima")).toBeInTheDocument();
    expect(screen.queryByText("Buzdolabı")).not.toBeInTheDocument();

    await user.click(screen.getByRole("tab", { name: "Mutfak" }));
    await screen.findByText("Buzdolabı");
    await user.type(screen.getByLabelText("Cihaz ara"), "klima");

    await waitFor(() => expect(screen.queryByText("Buzdolabı")).not.toBeInTheDocument());
    expect(screen.queryByText("Klima")).not.toBeInTheDocument();
  });

  it("opens the details drawer and adds the appliance from its CTA", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    await user.click(screen.getByText("Buzdolabı"));
    expect(await screen.findByRole("button", { name: "Bu cihazı ekle" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Bu cihazı ekle" }));
    expect(await screen.findByText("Seçilen Cihazlar (1)")).toBeInTheDocument();
  });

  it("submits the built payload with catalogItemId and shows a success state", async () => {
    let resolveRegister: (value: { homeId: string; name: string; contactEmail: string }) => void = () => {};
    vi.mocked(registerHome).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRegister = resolve;
        }),
    );

    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    await addRefrigerator(user);
    await screen.findByText("Seçilen Cihazlar (1)");
    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByRole("button", { name: "Evi ve cihazları kaydet" }));

    expect(await screen.findByText("Ev kaydediliyor…")).toBeInTheDocument();
    expect(vi.mocked(registerHome).mock.calls[0][0]).toEqual(
      expect.objectContaining({
        name: "Bahçelievler Dairesi",
        contactEmail: "test@example.com",
        appliances: [
          expect.objectContaining({
            name: "Buzdolabı",
            type: "REFRIGERATOR",
            catalogItemId: "cat-refrigerator",
            safePowerLimitWatt: null,
            simulationMinWatt: null,
            simulationMaxWatt: null,
          }),
        ],
      }),
    );

    resolveRegister({ homeId: "home-1", name: "Bahçelievler Dairesi", contactEmail: "test@example.com" });

    expect(await screen.findByText("Ev başarıyla kaydedildi")).toBeInTheDocument();
  });

  it("sends the edited value for a field the user touched, but leaves untouched fields null", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    await addRefrigerator(user);
    await screen.findByText("Seçilen Cihazlar (1)");

    // Expand the tray group, then the per-instance advanced-settings panel.
    await user.click(screen.getByRole("button", { name: "Buzdolabı" }));
    await user.click(screen.getByText("Gelişmiş ayarlar"));

    const limitInput = screen.getByLabelText("Güvenli limit (W)");
    await user.clear(limitInput);
    await user.type(limitInput, "250");

    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByRole("button", { name: "Evi ve cihazları kaydet" }));

    expect(vi.mocked(registerHome).mock.calls[0][0]).toEqual(
      expect.objectContaining({
        appliances: [
          expect.objectContaining({
            safePowerLimitWatt: 250,
            simulationMinWatt: null,
            simulationMaxWatt: null,
          }),
        ],
      }),
    );
  });

  it("shows the API error message when registration fails", async () => {
    vi.mocked(registerHome).mockRejectedValue(new ApiError("Bu e-posta zaten kullanılıyor."));

    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);
    await addRefrigerator(user);
    await screen.findByText("Seçilen Cihazlar (1)");
    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByRole("button", { name: "Evi ve cihazları kaydet" }));

    expect(await screen.findByText("Bu e-posta zaten kullanılıyor.")).toBeInTheDocument();
  });

  it("increments catalog item quantity from the tray, producing that many independently named instances", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    await addRefrigerator(user);
    await screen.findByText("Seçilen Cihazlar (1)");
    await user.click(screen.getByRole("button", { name: "Buzdolabı adedini artır" }));
    await screen.findByText("Seçilen Cihazlar (2)");

    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByRole("button", { name: "Evi ve cihazları kaydet" }));

    const appliances = vi.mocked(registerHome).mock.calls[0][0].appliances;
    expect(appliances).toHaveLength(2);
    expect(appliances.map((a) => a.name)).toEqual(["Buzdolabı", "Buzdolabı 2"]);
    expect(appliances.every((a) => a.catalogItemId === "cat-refrigerator")).toBe(true);
  });

  it("decrements catalog item quantity from the tray, removing the most recently added instance", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    await addRefrigerator(user);
    await user.click(screen.getByRole("button", { name: "Buzdolabı adedini artır" }));
    await screen.findByText("Seçilen Cihazlar (2)");

    await user.click(screen.getByRole("button", { name: "Buzdolabı adedini azalt" }));
    expect(await screen.findByText("Seçilen Cihazlar (1)")).toBeInTheDocument();
  });

  it("allows adding a custom appliance, always sending its own watt fields", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    await user.click(screen.getByRole("button", { name: "Cihaz ekle" }));
    expect(screen.getByPlaceholderText("Cihaz adı")).toBeInTheDocument();

    await user.type(screen.getByPlaceholderText("Cihaz adı"), "Özel Cihaz");
    const typeInput = screen.getByPlaceholderText("Tip (örn. HEATER)");
    await user.clear(typeInput);
    await user.type(typeInput, "CUSTOM_TYPE");
    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByRole("button", { name: "Evi ve cihazları kaydet" }));

    expect(vi.mocked(registerHome).mock.calls[0][0]).toEqual(
      expect.objectContaining({
        appliances: [
          expect.objectContaining({
            name: "Özel Cihaz",
            type: "CUSTOM_TYPE",
            catalogItemId: null,
            safePowerLimitWatt: 500,
            simulationMinWatt: 100,
            simulationMaxWatt: 500,
          }),
        ],
      }),
    );
  });

  it("keeps the tray selection intact across a background catalog refetch", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);
    await goToAppliancesStep(user);

    await addRefrigerator(user);
    await screen.findByText("Seçilen Cihazlar (1)");

    // Simulate a background refetch returning a fresh array reference with the same content — the
    // selection now lives in `useCatalogSelection`, entirely decoupled from the catalog query, so
    // it must never be reset by this.
    vi.mocked(fetchApplianceCatalog).mockResolvedValueOnce([...CATALOG_FIXTURE]);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(screen.getByText("Seçilen Cihazlar (1)")).toBeInTheDocument();
  });
});
