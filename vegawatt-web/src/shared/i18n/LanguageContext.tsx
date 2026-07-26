import { createContext, useContext, useState, type ReactNode } from "react";

export type Language = "tr" | "en";

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: string) => string;
}

const TRANSLATIONS: Record<Language, Record<string, string>> = {
  tr: {
    // Nav
    "nav.overview": "Genel Bakış",
    "nav.homes": "Evlerim",
    "nav.devices": "Cihazlarım",
    "nav.assistant": "AI Asistan",
    "nav.notifications": "Bildirimler",
    "nav.userManagement": "Kullanıcı Yönetimi",
    "nav.systemLive": "Sistem canlı",
    "nav.systemDisconnected": "Bağlantı kesildi",
    "nav.logout": "Çıkış",

    // View Modes
    "view.grid": "Kart Görünümü",
    "view.list": "Liste Görünümü",

    // Health Strip & Badges
    "health.normal": "Normal",
    "health.approachingQuota": "Kota yaklaşıyor",
    "health.quotaExceeded": "Kota aşıldı",
    "health.penaltyTariff": "Yüksek tarife",
    "health.anomaly": "Anomali",
    "health.highConsumption": "Yüksek tüketim",

    // KPIs
    "kpi.registeredHomes": "Kayıtlı Evler",
    "kpi.totalEnergy": "Toplam Güncel Enerji",
    "kpi.totalCost": "Toplam Güncel Maliyet",
    "kpi.attentionHomes": "Dikkat Gerektiren Evler",

    // Filters
    "filter.searchPlaceholder": "Ev ara",
    "filter.allStatus": "Tüm durumlar",
    "filter.allTariffs": "Tüm tarifeler",
    "filter.critical": "En kritik",
    "filter.costDesc": "Maliyet (Yüksek > Düşük)",
    "filter.energyDesc": "Enerji (Yüksek > Düşük)",
    "filter.recent": "En yeni",
    "filter.name": "İsim (A-Z)",
    "filter.all": "Tümü",

    // Home & Device Cards
    "card.currentCost": "GÜNCEL MALİYET",
    "card.totalEnergy": "Toplam enerji",
    "card.energyQuota": "Enerji Kotası",
    "card.budgetQuota": "Bütçe Kotası",
    "card.lastData": "Son veri:",
    "card.standardTariff": "Standart tarife",
    "card.penaltyTariff": "Cezalı tarife",
    "card.newHome": "+ Yeni Ev",
    "card.currentPower": "Güncel Güç",
    "card.operatingMode": "Çalışma Modu",
    "card.share": "Pay",

    // Tables
    "table.home": "Ev",
    "table.tariff": "Tarife",
    "table.currentPower": "Güncel Güç",
    "table.totalEnergy": "Toplam Enerji",
    "table.currentCost": "Güncel Maliyet",
    "table.quotas": "Kotalar",
    "table.status": "Durum",
    "table.lastData": "Son Veri",

    // Landing
    "landing.heroBadge": "Akıllı Ev Enerji & Telemetri Platformu",
    "landing.heroTitle1": "Enerjiyi yalnız izlemeyin.",
    "landing.heroTitle2": "Nereye gittiğini anlayın.",
    "landing.heroSubtitle": "Canlı tüketim takibi, bütçe tahmini, cihaz bazlı anomali tespiti ve AI destekli tasarruf önerileri tek panelde.",
    "landing.scrollDown": "Aşağı Kaydırın & Rehberi İnceleyin",
    "landing.howItWorks": "Kullanım Kılavuzu & Rehber",
    "landing.threeSteps": "Üç adımda tam kontrol",
    "landing.features": "Özellikler",
    "landing.login": "Giriş Yap",
    "landing.register": "Hesap Oluştur",

    // Footer
    "footer.copyright": "© 2026 VegaWatt",
    "footer.tagline": "Akıllı Ev Enerji Yönetimi & Telemetri Platformu",
    "footer.team": "Geliştirici Ekip:",
  },
  en: {
    // Nav
    "nav.overview": "Overview",
    "nav.homes": "My Homes",
    "nav.devices": "My Devices",
    "nav.assistant": "AI Assistant",
    "nav.notifications": "Notifications",
    "nav.userManagement": "User Management",
    "nav.systemLive": "System Live",
    "nav.systemDisconnected": "Disconnected",
    "nav.logout": "Log Out",

    // View Modes
    "view.grid": "Grid View",
    "view.list": "List View",

    // Health Strip & Badges
    "health.normal": "Normal",
    "health.approachingQuota": "Approaching quota",
    "health.quotaExceeded": "Quota exceeded",
    "health.penaltyTariff": "High tariff",
    "health.anomaly": "Anomaly",
    "health.highConsumption": "High power draw",

    // KPIs
    "kpi.registeredHomes": "Registered Homes",
    "kpi.totalEnergy": "Total Live Energy",
    "kpi.totalCost": "Total Live Cost",
    "kpi.attentionHomes": "Homes Requiring Attention",

    // Filters
    "filter.searchPlaceholder": "Search homes...",
    "filter.allStatus": "All status",
    "filter.allTariffs": "All tariffs",
    "filter.critical": "Most critical",
    "filter.costDesc": "Cost (High > Low)",
    "filter.energyDesc": "Energy (High > Low)",
    "filter.recent": "Most recent",
    "filter.name": "Name (A-Z)",
    "filter.all": "All",

    // Home & Device Cards
    "card.currentCost": "CURRENT COST",
    "card.totalEnergy": "Total energy",
    "card.energyQuota": "Energy Quota",
    "card.budgetQuota": "Budget Quota",
    "card.lastData": "Last data:",
    "card.standardTariff": "Standard tariff",
    "card.penaltyTariff": "Penalty tariff",
    "card.newHome": "+ Add Home",
    "card.currentPower": "Current Power",
    "card.operatingMode": "Operating Mode",
    "card.share": "Share",

    // Tables
    "table.home": "Home",
    "table.tariff": "Tariff",
    "table.currentPower": "Current Power",
    "table.totalEnergy": "Total Energy",
    "table.currentCost": "Current Cost",
    "table.quotas": "Quotas",
    "table.status": "Status",
    "table.lastData": "Last Data",

    // Landing
    "landing.heroBadge": "Smart Home Energy & Telemetry Platform",
    "landing.heroTitle1": "Don't just watch energy.",
    "landing.heroTitle2": "Understand where it goes.",
    "landing.heroSubtitle": "Real-time consumption tracking, budget forecasting, appliance anomaly detection, and AI energy advisory in one panel.",
    "landing.scrollDown": "Scroll Down & Explore Guide",
    "landing.howItWorks": "User Guide & Onboarding",
    "landing.threeSteps": "Full control in three steps",
    "landing.features": "Key Features",
    "landing.login": "Sign In",
    "landing.register": "Create Account",

    // Footer
    "footer.copyright": "© 2026 VegaWatt",
    "footer.tagline": "Smart Home Energy Management & Telemetry Platform",
    "footer.team": "Developer Team:",
  },
};

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() => {
    const saved = localStorage.getItem("vegawatt_lang");
    return saved === "en" ? "en" : "tr";
  });

  function setLanguage(lang: Language) {
    setLanguageState(lang);
    localStorage.setItem("vegawatt_lang", lang);
  }

  function t(key: string): string {
    return TRANSLATIONS[language][key] ?? TRANSLATIONS.tr[key] ?? key;
  }

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error("useLanguage must be used within a LanguageProvider");
  }
  return context;
}
