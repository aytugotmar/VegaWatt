import { apiFetch } from "./client";
import type {
  ConsumptionHistoryPoint,
  HomeLiveStatus,
  HomeLiveSummary,
  OperationalEvent,
  Recommendation,
  RegisterHomeRequest,
  RegisterHomeResponse,
} from "../types/home";

export function fetchLiveHomes(): Promise<HomeLiveSummary[]> {
  return apiFetch<HomeLiveSummary[]>("/api/v1/homes/live");
}

export function fetchLiveHome(homeId: string): Promise<HomeLiveStatus> {
  return apiFetch<HomeLiveStatus>(`/api/v1/homes/${homeId}/live`);
}

export function fetchHomeHistory(homeId: string, from?: string, to?: string): Promise<ConsumptionHistoryPoint[]> {
  const params = new URLSearchParams();
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  const query = params.toString();
  return apiFetch<ConsumptionHistoryPoint[]>(`/api/v1/homes/${homeId}/history${query ? `?${query}` : ""}`);
}

export function fetchHomeRecommendations(homeId: string): Promise<Recommendation[]> {
  return apiFetch<Recommendation[]>(`/api/v1/homes/${homeId}/recommendations`);
}

export function fetchHomeEvents(homeId: string): Promise<OperationalEvent[]> {
  return apiFetch<OperationalEvent[]>(`/api/v1/homes/${homeId}/events`);
}

export function registerHome(request: RegisterHomeRequest): Promise<RegisterHomeResponse> {
  return apiFetch<RegisterHomeResponse>("/api/v1/homes", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
