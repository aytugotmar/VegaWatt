import { refreshSession } from "../api/authApi";
import { notifySessionExpired, setAccessToken } from "./tokenProvider";

let inFlight: Promise<string | null> | null = null;

export function refreshAccessToken(): Promise<string | null> {
  if (!inFlight) {
    inFlight = refreshSession()
      .then((session) => {
        setAccessToken(session.accessToken);
        return session.accessToken;
      })
      .catch(() => {
        setAccessToken(null);
        notifySessionExpired();
        return null;
      })
      .finally(() => {
        inFlight = null;
      });
  }
  return inFlight;
}
