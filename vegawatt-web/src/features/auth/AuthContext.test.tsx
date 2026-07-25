import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";

vi.mock("../../shared/api/authApi", () => ({
  refreshSession: vi.fn(),
  loginUser: vi.fn(),
  registerUser: vi.fn(),
  logoutUser: vi.fn(),
}));

import { loginUser, logoutUser, refreshSession } from "../../shared/api/authApi";

function TestConsumer() {
  const { user, isInitializing, login, logout } = useAuth();
  if (isInitializing) return <span>loading</span>;
  return (
    <div>
      <span data-testid="user-state">{user ? user.email : "signed-out"}</span>
      <button onClick={() => void login("user@vegawatt.com", "password123")}>login</button>
      <button onClick={() => void logout()}>logout</button>
    </div>
  );
}

function renderAuth() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

describe("AuthContext logout", () => {
  beforeEach(() => {
    vi.mocked(refreshSession).mockRejectedValue(new Error("no session"));
  });

  it("clears local user state even when the server logout call fails", async () => {
    vi.mocked(loginUser).mockResolvedValue({
      userId: "u1",
      email: "user@vegawatt.com",
      role: "USER",
      accessToken: "token",
    });
    vi.mocked(logoutUser).mockRejectedValue(new Error("network down"));

    const user = userEvent.setup();
    renderAuth();

    await waitFor(() => expect(screen.getByTestId("user-state")).toHaveTextContent("signed-out"));

    await user.click(screen.getByText("login"));
    await waitFor(() => expect(screen.getByTestId("user-state")).toHaveTextContent("user@vegawatt.com"));

    await user.click(screen.getByText("logout"));

    await waitFor(() => expect(screen.getByTestId("user-state")).toHaveTextContent("signed-out"));
  });
});
