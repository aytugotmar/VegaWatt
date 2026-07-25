import { ApiError } from "./client";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export interface AuthResponse {
  userId: string;
  email: string;
  role: "USER" | "ADMIN";
  accessToken: string;
}

interface ProblemDetail {
  title?: string;
  detail?: string;
}

async function authFetch(path: string, init?: RequestInit): Promise<AuthResponse> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      credentials: "include",
      headers: { "Content-Type": "application/json", Accept: "application/json", ...init?.headers },
    });
  } catch {
    throw new ApiError("Sunucuya bağlanılamadı. Lütfen bağlantınızı kontrol edin.");
  }

  if (!response.ok) {
    let message = `İstek başarısız oldu (${response.status}).`;
    try {
      const problem: ProblemDetail = await response.json();
      message = problem.detail ?? problem.title ?? message;
    } catch {
      // response body was not JSON, keep default message
    }
    throw new ApiError(message, response.status);
  }
  return (await response.json()) as AuthResponse;
}

export function registerUser(email: string, password: string): Promise<AuthResponse> {
  return authFetch("/api/v1/auth/register", { method: "POST", body: JSON.stringify({ email, password }) });
}

export function loginUser(email: string, password: string): Promise<AuthResponse> {
  return authFetch("/api/v1/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
}

export function refreshSession(): Promise<AuthResponse> {
  return authFetch("/api/v1/auth/refresh", { method: "POST" });
}

export async function logoutUser(): Promise<void> {
  await fetch(`${API_BASE_URL}/api/v1/auth/logout`, { method: "POST", credentials: "include" });
}
