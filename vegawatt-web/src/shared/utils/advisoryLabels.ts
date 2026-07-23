const TRIGGER_LABELS: Record<string, string> = {
  QUOTA_80: "Kota Uyarısı (%80)",
  QUOTA_100: "Kota Aşımı (%100)",
  ANOMALY: "Anomali Tespiti",
  RECOVERY: "Normale Dönüş",
};

/**
 * The backend's AdvisoryTriggerType currently only emits QUOTA_80 / QUOTA_100 / ANOMALY —
 * RECOVERY is mapped here for forward-compatibility per the design brief but never actually
 * fires today (no notification job is created on anomaly recovery).
 */
export function getAdvisoryTriggerLabel(triggerType: string): string {
  return TRIGGER_LABELS[triggerType] ?? "Sistem Önerisi";
}

const TRIGGER_EXPLANATIONS: Record<string, string> = {
  QUOTA_80: "Enerji veya bütçe kotasının %80'ine ulaşıldığında gönderilir.",
  QUOTA_100: "Enerji veya bütçe kotası aşıldığında gönderilir; bütçe aşımında ceza tarifesi devreye girer.",
  ANOMALY: "Bir cihaz güvenli güç sınırını 3 ölçüm üst üste aştığında gönderilir.",
  RECOVERY: "Anormal bir cihaz güvenli sınırın altına döndüğünde gönderilir.",
};

/** A one-line, trigger-type-level explanation of *why this category of recommendation exists* —
 * deliberately not tied to the appliance's numbers at the moment the recommendation fired, since
 * the backend doesn't attach those to the recommendation record and showing today's live numbers
 * next to a possibly-old recommendation would misrepresent what actually triggered it. */
export function getAdvisoryTriggerExplanation(triggerType: string): string {
  return TRIGGER_EXPLANATIONS[triggerType] ?? "Sistem tarafından otomatik olarak oluşturuldu.";
}

export type AdvisorySeverity = "CRITICAL" | "WARNING" | "INFO";

const TRIGGER_SEVERITY: Record<string, AdvisorySeverity> = {
  QUOTA_80: "WARNING",
  QUOTA_100: "CRITICAL",
  ANOMALY: "CRITICAL",
  RECOVERY: "INFO",
};

export function getAdvisorySeverity(triggerType: string): AdvisorySeverity {
  return TRIGGER_SEVERITY[triggerType] ?? "INFO";
}

const SEVERITY_LABELS: Record<AdvisorySeverity, string> = {
  CRITICAL: "Kritik",
  WARNING: "Uyarı",
  INFO: "Bilgi",
};

export function getSeverityLabel(severity: AdvisorySeverity): string {
  return SEVERITY_LABELS[severity];
}
