import { refreshAccessToken } from "../auth/refreshCoordinator";
import { getAccessToken } from "../auth/tokenProvider";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message);
    this.name = "ApiError";
  }
}

interface ProblemDetail {
  title?: string;
  detail?: string;
}

export async function apiFetch<T>(path: string, init?: RequestInit, isRetry = false): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");
  if (init?.body) {
    headers.set("Content-Type", "application/json");
  }
  const accessToken = getAccessToken();
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers, credentials: "include" });
  } catch {
    throw new ApiError("Sunucuya bağlanılamadı. Lütfen bağlantınızı kontrol edin.");
  }

  if (response.status === 401 && !isRetry) {
    const newAccessToken = await refreshAccessToken();
    if (newAccessToken) {
      return apiFetch<T>(path, init, true);
    }
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

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
