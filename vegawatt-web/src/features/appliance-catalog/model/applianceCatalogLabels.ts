// Backend sends only technical enum strings (ApplianceBehaviorProfile, ApplianceTriggerType) — the
// user must never see them raw (yapılacak.md §20.4/§28). These maps translate them to short,
// user-facing Turkish labels.

const BEHAVIOR_PROFILE_LABELS: Record<string, string> = {
  ALWAYS_ON_STABLE: "Sürekli açık",
  ALWAYS_ON_VARIABLE: "Sürekli açık, değişken güç",
  MANUAL_SWITCH: "Elle açılıp kapatılır",
  SCHEDULED_SWITCH: "Zamanlanmış çalışma",
  STANDBY_DEVICE: "Bekleme modu destekli",
  SHORT_SESSION: "Kısa süreli kullanım",
  SHORT_HIGH_POWER: "Kısa süreli yüksek güç",
  PROGRAM_CYCLE: "Program döngüsü",
  THERMOSTATIC_CYCLE: "Termostat kontrollü",
  THERMOSTATIC_SESSION: "Termostat kontrollü oturum",
  VARIABLE_LOAD: "Değişken yük",
  CHARGING_CURVE: "Şarj eğrisi",
  CHARGING_AND_SESSION: "Şarj ve kullanım oturumu",
  FLOW_TRIGGERED: "Kullanıma bağlı tetiklenir",
};

const TRIGGER_TYPE_LABELS: Record<string, string> = {
  SAFE_POWER_LIMIT_BREACHED: "Aşırı güç",
  CONSECUTIVE_BREACH_THRESHOLD_REACHED: "Sürekli aşırı güç",
  UNUSUAL_STANDBY_CONSUMPTION: "Yüksek bekleme tüketimi",
  SESSION_DURATION_EXCEEDED: "Normalden uzun çalışma",
  EXCESSIVE_DUTY_CYCLE: "Aşırı sık çalışma",
  UNEXPECTED_ACTIVE_PERIOD: "Beklenmeyen çalışma",
  EXPECTED_ACTIVITY_MISSING: "Beklenen çalışma eksik",
  TELEMETRY_STALE: "Veri gecikmesi",
  TELEMETRY_OFFLINE: "Bağlantı kesintisi",
  TELEMETRY_RESUMED: "Bağlantı yeniden kuruldu",
  ANOMALY_RECOVERED: "Normale döndü",
};

function titleCaseFallback(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

export function getBehaviorProfileLabel(behaviorProfile: string): string {
  return BEHAVIOR_PROFILE_LABELS[behaviorProfile] ?? titleCaseFallback(behaviorProfile);
}

export function getTriggerTypeLabel(triggerType: string): string {
  return TRIGGER_TYPE_LABELS[triggerType] ?? titleCaseFallback(triggerType);
}
