const EVENT_TYPE_LABELS: Record<string, string> = {
  ENERGY_QUOTA_80_REACHED: "Enerji kotası %80'e ulaştı",
  ENERGY_QUOTA_100_REACHED: "Enerji kotası aşıldı",
  BUDGET_QUOTA_80_REACHED: "Bütçe %80'e ulaştı",
  BUDGET_QUOTA_100_REACHED: "Bütçe aşıldı",
  PENALTY_TARIFF_ACTIVATED: "Ceza tarifesi devreye girdi",
  APPLIANCE_ANOMALY_DETECTED: "Cihaz anomalisi tespit edildi",
  APPLIANCE_ANOMALY_RECOVERED: "Cihaz normale döndü",
  APPLIANCE_STANDBY_ANOMALY_DETECTED: "Yüksek bekleme tüketimi tespit edildi",
  APPLIANCE_STANDBY_ANOMALY_RECOVERED: "Bekleme tüketimi normale döndü",
  APPLIANCE_TELEMETRY_STALE: "Veri gecikmeli",
  APPLIANCE_TELEMETRY_OFFLINE: "Bağlantı kesildi",
  APPLIANCE_TELEMETRY_RESUMED: "Bağlantı yeniden kuruldu",
};

export function getEventTypeLabel(eventType: string): string {
  return EVENT_TYPE_LABELS[eventType] ?? "Sistem olayı";
}

export type EventTone = "danger" | "warning" | "success" | "info";

const EVENT_TONE: Record<string, EventTone> = {
  ENERGY_QUOTA_80_REACHED: "warning",
  ENERGY_QUOTA_100_REACHED: "danger",
  BUDGET_QUOTA_80_REACHED: "warning",
  BUDGET_QUOTA_100_REACHED: "danger",
  PENALTY_TARIFF_ACTIVATED: "danger",
  APPLIANCE_ANOMALY_DETECTED: "danger",
  APPLIANCE_ANOMALY_RECOVERED: "success",
  APPLIANCE_STANDBY_ANOMALY_DETECTED: "danger",
  APPLIANCE_STANDBY_ANOMALY_RECOVERED: "success",
  APPLIANCE_TELEMETRY_STALE: "warning",
  APPLIANCE_TELEMETRY_OFFLINE: "danger",
  APPLIANCE_TELEMETRY_RESUMED: "success",
};

/** Reuses the same 4-tone system as `advisoryLabels.ts`'s severity classes — no new colors. */
export function getEventTone(eventType: string): EventTone {
  return EVENT_TONE[eventType] ?? "info";
}
