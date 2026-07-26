import { useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { loginUser, logoutUser, registerUser, refreshSession, type AuthResponse } from "../../shared/api/authApi";
import { onSessionExpired, setAccessToken } from "../../shared/auth/tokenProvider";

export interface CurrentUser {
  userId: string;
  email: string;
  role: "USER" | "ADMIN";
}

interface AuthContextValue {
  user: CurrentUser | null;
  isInitializing: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  updateCurrentUser: (patch: Partial<CurrentUser>) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function toCurrentUser(session: AuthResponse): CurrentUser {
  return { userId: session.userId, email: session.email, role: session.role };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const queryClient = useQueryClient();

  useEffect(() => {
    refreshSession()
      .then((session) => {
        setAccessToken(session.accessToken);
        setUser(toCurrentUser(session));
      })
      .catch(() => {
        setAccessToken(null);
        setUser(null);
      })
      .finally(() => setIsInitializing(false));
  }, []);

  useEffect(
    () =>
      onSessionExpired(() => {
        setUser(null);
        queryClient.clear();
      }),
    [queryClient],
  );

  const login = useCallback(async (email: string, password: string) => {
    const session = await loginUser(email, password);
    setAccessToken(session.accessToken);
    setUser(toCurrentUser(session));
  }, []);

  const register = useCallback(async (email: string, password: string) => {
    const session = await registerUser(email, password);
    setAccessToken(session.accessToken);
    setUser(toCurrentUser(session));
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutUser();
    } catch {
      // Best-effort: even if the server call fails (offline, timeout, 5xx), the user still
      // clicked logout and expects the client to forget them immediately.
    } finally {
      setAccessToken(null);
      setUser(null);
      queryClient.clear();
    }
  }, [queryClient]);

  const updateCurrentUser = useCallback((patch: Partial<CurrentUser>) => {
    setUser((current) => (current ? { ...current, ...patch } : current));
  }, []);

  const value = useMemo(
    () => ({ user, isInitializing, login, register, logout, updateCurrentUser }),
    [user, isInitializing, login, register, logout, updateCurrentUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
