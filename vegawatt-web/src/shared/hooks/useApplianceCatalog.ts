import { useQuery } from "@tanstack/react-query";
import { fetchApplianceCatalog } from "../api/applianceCatalogApi";

const CATALOG_STALE_TIME_MS = 10 * 60 * 1000;

export function useApplianceCatalogQuery() {
  return useQuery({
    queryKey: ["applianceCatalog"],
    queryFn: fetchApplianceCatalog,
    staleTime: CATALOG_STALE_TIME_MS,
  });
}
