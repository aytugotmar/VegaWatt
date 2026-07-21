import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchHomeHistory,
  fetchHomeRecommendations,
  fetchLiveHome,
  fetchLiveHomes,
  registerHome,
} from "../api/homesApi";

const LIVE_HOMES_INTERVAL_MS = 2000;
const RECOMMENDATIONS_INTERVAL_MS = 20_000;

export function useLiveHomesQuery() {
  return useQuery({
    queryKey: ["homes", "live"],
    queryFn: fetchLiveHomes,
    refetchInterval: LIVE_HOMES_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });
}

export function useLiveHomeQuery(homeId: string | undefined) {
  return useQuery({
    queryKey: ["homes", homeId, "live"],
    queryFn: () => fetchLiveHome(homeId as string),
    enabled: !!homeId,
    refetchInterval: LIVE_HOMES_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });
}

export function useHomeHistoryQuery(homeId: string | undefined, from: string, to: string) {
  return useQuery({
    queryKey: ["homes", homeId, "history", from, to],
    queryFn: () => fetchHomeHistory(homeId as string, from, to),
    enabled: !!homeId,
  });
}

export function useRecommendationsQuery(homeId: string | undefined) {
  return useQuery({
    queryKey: ["homes", homeId, "recommendations"],
    queryFn: () => fetchHomeRecommendations(homeId as string),
    enabled: !!homeId,
    refetchInterval: RECOMMENDATIONS_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });
}

export function useRegisterHomeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: registerHome,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["homes", "live"] });
    },
  });
}
