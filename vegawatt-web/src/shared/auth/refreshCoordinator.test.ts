import { describe, expect, it, vi, beforeEach } from "vitest";

vi.mock("../api/authApi", () => ({
  refreshSession: vi.fn(),
}));

import { refreshSession } from "../api/authApi";
import { refreshAccessToken } from "./refreshCoordinator";
import { getAccessToken, onSessionExpired, setAccessToken } from "./tokenProvider";

describe("refreshAccessToken", () => {
  beforeEach(() => {
    vi.mocked(refreshSession).mockReset();
    setAccessToken(null);
  });

  it("notifies session-expired subscribers and clears the access token when refresh fails", async () => {
    vi.mocked(refreshSession).mockRejectedValue(new Error("refresh token expired"));
    const listener = vi.fn();
    const unsubscribe = onSessionExpired(listener);

    const result = await refreshAccessToken();

    expect(result).toBeNull();
    expect(getAccessToken()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });

  it("does not notify session-expired subscribers when refresh succeeds", async () => {
    vi.mocked(refreshSession).mockResolvedValue({
      userId: "u1",
      email: "user@vegawatt.com",
      role: "USER",
      accessToken: "new-token",
    });
    const listener = vi.fn();
    const unsubscribe = onSessionExpired(listener);

    const result = await refreshAccessToken();

    expect(result).toBe("new-token");
    expect(getAccessToken()).toBe("new-token");
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });
});
