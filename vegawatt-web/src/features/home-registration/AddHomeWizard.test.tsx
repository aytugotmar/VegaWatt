import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../test/testUtils";
import { ApiError } from "../../shared/api/client";
import { AddHomeWizard } from "./AddHomeWizard";

vi.mock("../../shared/api/homesApi", () => ({
  fetchLiveHomes: vi.fn(),
  fetchLiveHome: vi.fn(),
  fetchHomeHistory: vi.fn(),
  fetchHomeRecommendations: vi.fn(),
  registerHome: vi.fn(),
}));

import { registerHome } from "../../shared/api/homesApi";

async function fillHomeInfoStep(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Ev adı"), "Bahçelievler Dairesi");
  await user.type(screen.getByLabelText("Bildirim e-postası"), "test@example.com");
  await user.click(screen.getByRole("button", { name: "İleri" }));
}

describe("AddHomeWizard", () => {
  beforeEach(() => {
    vi.mocked(registerHome).mockReset();
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

  it("submits the built payload, shows a loading state, and then a success state", async () => {
    let resolveRegister: (value: { homeId: string; name: string; contactEmail: string }) => void = () => {};
    vi.mocked(registerHome).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRegister = resolve;
        }),
    );

    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);

    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "İleri" }));

    await user.click(screen.getByText("Buzdolabı").closest("label")!.querySelector("input[type=checkbox]")!);
    await user.click(screen.getByRole("button", { name: "İleri" }));

    await screen.findByText("Cihazlar (1)");
    await user.click(screen.getByRole("button", { name: "Evi ve cihazları kaydet" }));

    expect(await screen.findByText("Ev kaydediliyor…")).toBeInTheDocument();
    expect(vi.mocked(registerHome).mock.calls[0][0]).toEqual(
      expect.objectContaining({
        name: "Bahçelievler Dairesi",
        contactEmail: "test@example.com",
        appliances: [expect.objectContaining({ type: "REFRIGERATOR" })],
      }),
    );

    resolveRegister({ homeId: "home-1", name: "Bahçelievler Dairesi", contactEmail: "test@example.com" });

    expect(await screen.findByText("Ev başarıyla kaydedildi")).toBeInTheDocument();
  });

  it("shows the API error message when registration fails", async () => {
    vi.mocked(registerHome).mockRejectedValue(new ApiError("Bu e-posta zaten kullanılıyor."));

    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);

    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByText("Buzdolabı").closest("label")!.querySelector("input[type=checkbox]")!);
    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByRole("button", { name: "Evi ve cihazları kaydet" }));

    expect(await screen.findByText("Bu e-posta zaten kullanılıyor.")).toBeInTheDocument();
  });

  it("increments preset quantity when the plus stepper is used", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);

    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByText("Buzdolabı").closest("label")!.querySelector("input[type=checkbox]")!);
    await user.click(screen.getByRole("button", { name: "Artır" }));

    expect(screen.getByTestId("preset-quantity-refrigerator")).toHaveTextContent("2");
  });

  it("allows adding a custom appliance", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddHomeWizard onClose={vi.fn()} />);

    await fillHomeInfoStep(user);
    await user.click(screen.getByRole("button", { name: "İleri" }));
    await user.click(screen.getByRole("button", { name: "Cihaz ekle" }));

    expect(screen.getByPlaceholderText("Cihaz adı")).toBeInTheDocument();
  });
});
