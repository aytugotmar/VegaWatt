import { apiFetch } from "./client";
import type { ApplianceCatalogItem } from "../types/applianceCatalog";

export function fetchApplianceCatalog(): Promise<ApplianceCatalogItem[]> {
  return apiFetch<ApplianceCatalogItem[]>("/api/v1/appliance-catalog");
}
