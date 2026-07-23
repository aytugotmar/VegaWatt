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
  it("points each header/CTA anchor at its own distinct, existing section — not a shared one", async () => {
    renderLandingPage();
    await screen.findByText("Canlı tüketim takibi");

    const howItWorksLink = screen.getByRole("link", { name: "Nasıl Çalışır" });
    const featuresLink = screen.getByRole("link", { name: "Özellikler" });
    const demoLink = screen.getByRole("link", { name: "Demoyu incele" });

    const targets = [howItWorksLink, featuresLink, demoLink].map((link) => link.getAttribute("href"));
    // Every anchor must target a different section — this is exactly the bug that made "Özellikler"
    // and "Demoyu incele" feel broken: both pointed at #ozellikler, which sat on content already
    // visible in the hero, so clicking either produced no perceptible scroll.
    expect(new Set(targets).size).toBe(targets.length);

    for (const href of targets) {
      const id = href!.slice(1);
      expect(document.getElementById(id)).not.toBeNull();
    }
  });

  it("renders a real Özellikler section distinct from the demo preview and the how-it-works steps", async () => {
    renderLandingPage();

    expect(await screen.findByText("Canlı tüketim takibi")).toBeInTheDocument();
    expect(document.getElementById("ozellikler")).not.toBeNull();
    expect(document.getElementById("demo")).not.toBeNull();
    expect(document.getElementById("nasil-calisir")).not.toBeNull();
  });
});
