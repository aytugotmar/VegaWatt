let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

type SessionExpiredListener = () => void;
const sessionExpiredListeners = new Set<SessionExpiredListener>();

/** AuthContext subscribes so its `user` state gets cleared when a refresh fails outside of
 * its own initial-load effect (e.g. a mid-session 401 triggering refreshCoordinator) — without
 * this, the UI would keep showing a signed-in user while every subsequent request 401s. */
export function onSessionExpired(listener: SessionExpiredListener): () => void {
  sessionExpiredListeners.add(listener);
  return () => sessionExpiredListeners.delete(listener);
}

export function notifySessionExpired(): void {
  sessionExpiredListeners.forEach((listener) => listener());
}
