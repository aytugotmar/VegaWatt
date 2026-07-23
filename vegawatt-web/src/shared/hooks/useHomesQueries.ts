import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo } from "react";
import {
  fetchHomeEvents,
  fetchHomeHistory,
  fetchHomeRecommendations,
  fetchLiveHome,
  fetchLiveHomes,
  registerHome,
} from "../api/homesApi";
import type { HomeLiveStatus } from "../types/home";

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

/**
 * Fetches full per-appliance live status for every home in `homeIds` in parallel — used by the
 * personal overview page to aggregate total live power / top consumers across a user's (few)
 * homes. Shares its query key with `useLiveHomeQuery` so the cache is reused when a user drills
 * into a single home's detail page right after viewing the overview.
 */
export function useHomesLiveStatusQueries(homeIds: string[]) {
  return useQueries({
    queries: homeIds.map((homeId) => ({
      queryKey: ["homes", homeId, "live"],
      queryFn: () => fetchLiveHome(homeId),
      refetchInterval: LIVE_HOMES_INTERVAL_MS,
      refetchIntervalInBackground: false,
    })),
  });
}

export interface AllHomesLiveStatus {
  isLoading: boolean;
  isError: boolean;
  homes: HomeLiveStatus[];
}

/**
 * Combines the home list with a full-detail (per-appliance) fetch for each of the user's
 * (typically few) homes — the shared data source for any screen that needs to look across all
 * of a user's homes at once (personal overview, an all-devices list), not just one at a time.
 */
export function useAllHomesLiveStatus(): AllHomesLiveStatus {
  const { data: summaries, isLoading: summariesLoading, isError: summariesError } = useLiveHomesQuery();
  const homeIds = useMemo(() => (summaries ?? []).map((home) => home.homeId), [summaries]);
  const detailQueries = useHomesLiveStatusQueries(homeIds);

  return useMemo(() => {
    const homes = detailQueries.map((query) => query.data).filter((home): home is HomeLiveStatus => !!home);
    return {
      isLoading: summariesLoading || detailQueries.some((query) => query.isLoading),
      isError: summariesError || detailQueries.some((query) => query.isError),
      homes,
    };
  }, [detailQueries, summariesLoading, summariesError]);
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

export function useHomeEventsQuery(homeId: string | undefined) {
  return useQuery({
    queryKey: ["homes", homeId, "events"],
    queryFn: () => fetchHomeEvents(homeId as string),
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
