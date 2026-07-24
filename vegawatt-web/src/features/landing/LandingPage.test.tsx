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
  it("keeps the header nav to just the brand and login link, and points the demo CTA at an existing section", async () => {
    renderLandingPage();
    await screen.findByText("Canlı tüketim takibi");

    // The header intentionally no longer links directly to the "Nasıl Çalışır"/"Özellikler"
    // sections — they're still reachable by scrolling, but the top nav stays minimal.
    expect(screen.queryByRole("link", { name: "Nasıl Çalışır" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Özellikler" })).not.toBeInTheDocument();

    const demoLink = screen.getByRole("link", { name: "Demoyu incele" });
    const href = demoLink.getAttribute("href");
    expect(document.getElementById(href!.slice(1))).not.toBeNull();
  });

  it("renders a real Özellikler section distinct from the demo preview and the how-it-works steps", async () => {
    renderLandingPage();

    expect(await screen.findByText("Canlı tüketim takibi")).toBeInTheDocument();
    expect(document.getElementById("ozellikler")).not.toBeNull();
    expect(document.getElementById("demo")).not.toBeNull();
    expect(document.getElementById("nasil-calisir")).not.toBeNull();
  });
});
