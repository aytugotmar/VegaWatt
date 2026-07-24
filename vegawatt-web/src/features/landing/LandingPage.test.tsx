import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../test/testUtils";
import { AuthProvider } from "../auth/AuthContext";
import { LandingPage } from "./LandingPage";

vi.mock("../../shared/api/authApi", () => ({
  registerUser: vi.fn(),
  loginUser: vi.fn(),
  refreshSession: vi.fn().mockRejectedValue(new Error("no session")),
}));

function renderLandingPage() {
  return renderWithProviders(
    <AuthProvider>
      <LandingPage />
    </AuthProvider>,
  );
}

describe("LandingPage", () => {
  it("keeps the header to just the brand, with no top nav links or hero CTA buttons", async () => {
    renderLandingPage();
    await screen.findByText("Canlı tüketim takibi");

    // The header is brand-only now — no "Giriş Yap" link, and the hero's "Ücretsiz başla" /
    // "Demoyu incele" CTA buttons were removed; the embedded login/register card is the only
    // way in, and the demo preview is already visible inline without needing a jump link.
    expect(screen.queryByRole("link", { name: "Giriş Yap" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Nasıl Çalışır" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Özellikler" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Ücretsiz başla" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Demoyu incele" })).not.toBeInTheDocument();
  });

  it("renders a real Özellikler section distinct from the demo preview and the how-it-works steps", async () => {
    renderLandingPage();

    expect(await screen.findByText("Canlı tüketim takibi")).toBeInTheDocument();
    expect(document.getElementById("ozellikler")).not.toBeNull();
    expect(document.getElementById("demo")).not.toBeNull();
    expect(document.getElementById("nasil-calisir")).not.toBeNull();
  });
});
