import { createContext, useContext, useState, type ReactNode } from "react";

export type Language = "tr" | "en";

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
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

    // Overview
    "overview.title": "Genel Bakış",
    "overview.loading": "Genel bakış yükleniyor...",
    "overview.noHomesTitle": "Henüz kayıtlı ev yok",
    "overview.noHomesDesc": "İlk evinizi ekleyerek enerji tüketimini izlemeye başlayın.",
    "overview.goToHomes": "Evlerime git",
    "overview.livePower": "Canlı Güç",
    "overview.live": "Canlı",
    "overview.lastMinutes": "son {min} dakikada",
    "overview.sessionPeak": "Bu oturumdaki tepe: {peak}",
    "overview.gatheringData": "Veri toplanıyor…",
    "overview.budgetPacing": "Bütçe Gidişatı",
    "overview.budgetUsage": "Bütçe kullanımı",
    "overview.monthEndEstimate": "Tahmini ay sonu (basit projeksiyon)",
    "overview.homesStatus": "Evlerimin Durumu",
    "overview.liveCount": "canlı",
    "overview.staleCount": "stale",
    "overview.totalCount": "ev toplam",
    "overview.needsAttention": "Dikkat Gerektirenler",
    "overview.everythingNormal": "Her şey normal görünüyor.",
    "overview.topConsumers": "En Çok Tüketen Cihazlar",
    "overview.noDataYet": "Henüz veri yok.",

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
    "health.offline": "Çevrimdışı",
    "health.stale": "Veri bekleniyor",
    "health.standbyWarning": "Bekleme uyarısı",

    // Status Labels
    "status.normal": "Normal",
    "status.approachingQuota": "Kota yaklaşıyor",
    "status.quotaExceeded": "Kota aşıldı",
    "status.highTariff": "Yüksek tarife",
    "status.offline": "Çevrimdışı",
    "status.stale": "Veri bekleniyor",
    "status.anomaly": "Anomali",
    "status.standbyWarning": "Bekleme uyarısı",
    "status.details": "Detaylar",

    // KPIs
    "kpi.registeredHomes": "Kayıtlı Evler",
    "kpi.totalEnergy": "Toplam Güncel Enerji",
    "kpi.totalCost": "Toplam Güncel Maliyet",
    "kpi.attentionHomes": "Dikkat Gerektiren Evler",

    // Filters
    "filter.searchPlaceholder": "Ev ara...",
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

    // Devices Page
    "devices.title": "Cihazlarım",
    "devices.addAppliance": "Cihaz Ekle",
    "devices.noDevicesTitle": "Henüz cihaz yok",
    "devices.noDevicesDesc": "Evlerinize cihaz ekleyince burada listelenecek.",
    "devices.allHomes": "Tüm evler",
    "devices.allTypes": "Tüm cihaz tipleri",
    "devices.onlyAnomalous": "Sadece anomali",
    "devices.onlyNormal": "Sadece normal",
    "devices.whichHome": "Hangi eve ekleyelim?",
    "devices.device": "Cihaz",
    "devices.home": "Ev",
    "devices.limitUsage": "Limit kullanımı",
    "devices.highConsumption": "Yüksek tüketim",

    // Tables
    "table.home": "Ev",
    "table.tariff": "Tarife",
    "table.currentPower": "Güncel Güç",
    "table.totalEnergy": "Toplam Enerji",
    "table.currentCost": "Güncel Maliyet",
    "table.quotas": "Kotalar",
    "table.status": "Durum",
    "table.lastData": "Son Veri",

    // Auth & Login
    "auth.welcomeBack": "Tekrar hoş geldiniz",
    "auth.continueToPanel": "Enerji panelinize devam etmek için giriş yapın.",
    "auth.noAccount": "Hesabınız yok mu?",
    "auth.createFreeAccount": "Ücretsiz hesap oluşturun",
    "auth.hasAccount": "Zaten hesabınız var mı?",
    "auth.signIn": "Giriş Yap",
    "auth.signUp": "Hesap Oluştur",
    "auth.email": "E-posta",
    "auth.password": "Parola",
    "auth.passwordConfirm": "Parola Tekrarı",
    "auth.fullName": "Ad Soyad",
    "auth.loggingIn": "Giriş yapılıyor...",
    "auth.registering": "Kayıt olunuyor...",
    "auth.loginFailed": "Giriş yapılamadı.",
    "auth.heroTitle1": "Enerjiyi yalnız izlemeyin.",
    "auth.heroTitle2": "Nereye gittiğini anlayın.",
    "auth.heroSubtitle": "Canlı tüketim takibi, bütçe tahmini ve cihaz bazlı akıllı öneriler tek panelde.",
    "auth.highlight1": "Canlı tüketim verilerini saniyeler içinde izleyin",
    "auth.highlight2": "Aylık bütçenizi önceden tahmin edin",
    "auth.highlight3": "Cihaz bazlı akıllı tasarruf önerileri alın",
    "auth.copyright": "Tüm hakları saklıdır.",
    "auth.privacyNote": "Verileriniz yalnızca size ait evlerle sınırlandırılır.",

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
    "landing.liveTelemetry": "%100 Canlı Telemetri",
    "landing.aiAdvice": "Gemini AI Önerileri",
    "landing.anomalyAlert": "Anomali Uyarısı",
    "landing.step1Title": "Cihazlar Veri Üretir",
    "landing.step1Desc": "Evinizdeki her cihazın anlık güç tüketimi sürekli ölçülür ve güvenli limitler takip edilir.",
    "landing.step2Title": "VegaWatt Analiz Eder",
    "landing.step2Desc": "Tüketim geçmişi, bütçe hedefleri, kotanız ve anomaliler otomatik değerlendirilir.",
    "landing.step3Title": "Siz Aksiyon Alırsınız",
    "landing.step3Desc": "Kişiselleştirilmiş Gemini AI önerileriyle bütçenizi aşmadan tasarruf sağlarsınız.",
    "landing.feat1Title": "Canlı tüketim takibi",
    "landing.feat1Desc": "Her evin ve cihazın anlık gücü saniyeler içinde güncellenir.",
    "landing.feat2Title": "Bütçe ve Kota Tahmini",
    "landing.feat2Desc": "Ay sonunda ne kadar ödeyeceğinizi ve kotayı ne zaman aşacağınızı önceden görün.",
    "landing.feat3Title": "Cihaz Anomali Tespiti",
    "landing.feat3Desc": "Güvenli sınırı aşan bir cihaz olduğunda anında haberdar olun.",
    "landing.feat4Title": "Akıllı Öneri Motoru",
    "landing.feat4Desc": "Gemini destekli, cihaza özel Türkçe tasarruf önerileri e-postanıza gelir.",
    "landing.allInOne": "Enerjinizi yöneten her şey tek yerde",

    // Footer
    "footer.copyright": "© 2026 VegaWatt",
    "footer.tagline": "Akıllı Ev Enerji Yönetimi & Telemetri Platformu",
    "footer.team": "Geliştirici Ekip:",

    // Common
    "common.justNow": "az önce",
    "common.retry": "Tekrar dene",
    "common.cancel": "İptal",
    "common.save": "Kaydet",
    "common.add": "Ekle",
    "common.delete": "Sil",
    "common.loading": "Yükleniyor...",
    "common.close": "Kapat",
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

    // Overview
    "overview.title": "Overview",
    "overview.loading": "Loading overview...",
    "overview.noHomesTitle": "No registered homes yet",
    "overview.noHomesDesc": "Add your first home to start monitoring energy consumption.",
    "overview.goToHomes": "Go to My Homes",
    "overview.livePower": "Live Power",
    "overview.live": "Live",
    "overview.lastMinutes": "in the last {min} minutes",
    "overview.sessionPeak": "Session peak: {peak}",
    "overview.gatheringData": "Gathering data…",
    "overview.budgetPacing": "Budget Pacing",
    "overview.budgetUsage": "Budget usage",
    "overview.monthEndEstimate": "Estimated month-end (simple projection)",
    "overview.homesStatus": "My Homes Status",
    "overview.liveCount": "live",
    "overview.staleCount": "stale",
    "overview.totalCount": "homes total",
    "overview.needsAttention": "Needs Attention",
    "overview.everythingNormal": "Everything looks normal.",
    "overview.topConsumers": "Top Power Draw Appliances",
    "overview.noDataYet": "No data yet.",

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
    "health.offline": "Offline",
    "health.stale": "Awaiting data",
    "health.standbyWarning": "Standby warning",

    // Status Labels
    "status.normal": "Normal",
    "status.approachingQuota": "Approaching quota",
    "status.quotaExceeded": "Quota exceeded",
    "status.highTariff": "High tariff",
    "status.offline": "Offline",
    "status.stale": "Awaiting data",
    "status.anomaly": "Anomaly",
    "status.standbyWarning": "Standby warning",
    "status.details": "Details",

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

    // Devices Page
    "devices.title": "My Devices",
    "devices.addAppliance": "Add Device",
    "devices.noDevicesTitle": "No devices yet",
    "devices.noDevicesDesc": "Devices added to your homes will be listed here.",
    "devices.allHomes": "All homes",
    "devices.allTypes": "All device types",
    "devices.onlyAnomalous": "Anomalies only",
    "devices.onlyNormal": "Normal only",
    "devices.whichHome": "Which home would you like to add to?",
    "devices.device": "Device",
    "devices.home": "Home",
    "devices.limitUsage": "Limit usage",
    "devices.highConsumption": "High consumption",

    // Tables
    "table.home": "Home",
    "table.tariff": "Tariff",
    "table.currentPower": "Current Power",
    "table.totalEnergy": "Total Energy",
    "table.currentCost": "Current Cost",
    "table.quotas": "Quotas",
    "table.status": "Status",
    "table.lastData": "Last Data",

    // Auth & Login
    "auth.welcomeBack": "Welcome back",
    "auth.continueToPanel": "Sign in to continue to your energy dashboard.",
    "auth.noAccount": "Don't have an account?",
    "auth.createFreeAccount": "Create a free account",
    "auth.hasAccount": "Already have an account?",
    "auth.signIn": "Sign In",
    "auth.signUp": "Create Account",
    "auth.email": "Email",
    "auth.password": "Password",
    "auth.passwordConfirm": "Confirm Password",
    "auth.fullName": "Full Name",
    "auth.loggingIn": "Signing in...",
    "auth.registering": "Registering...",
    "auth.loginFailed": "Failed to sign in.",
    "auth.heroTitle1": "Don't just watch energy.",
    "auth.heroTitle2": "Understand where it goes.",
    "auth.heroSubtitle": "Real-time consumption tracking, budget forecasting, and AI advisory in one panel.",
    "auth.highlight1": "Monitor live power consumption data in seconds",
    "auth.highlight2": "Forecast your monthly budget in advance",
    "auth.highlight3": "Receive smart appliance-level energy saving tips",
    "auth.copyright": "All rights reserved.",
    "auth.privacyNote": "Your data is strictly isolated to your registered homes.",

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
    "landing.liveTelemetry": "100% Live Telemetry",
    "landing.aiAdvice": "Gemini AI Recommendations",
    "landing.anomalyAlert": "Anomaly Alerts",
    "landing.step1Title": "Devices Produce Telemetry",
    "landing.step1Desc": "Instant power draw for every appliance is measured continuously against safe limits.",
    "landing.step2Title": "VegaWatt Analyzes",
    "landing.step2Desc": "Historical trends, budget targets, quotas, and anomalies are evaluated automatically.",
    "landing.step3Title": "You Take Action",
    "landing.step3Desc": "Keep costs under control using personalized Gemini AI energy saving insights.",
    "landing.feat1Title": "Live consumption tracking",
    "landing.feat1Desc": "Instant power draw for every home and device updates in seconds.",
    "landing.feat2Title": "Budget & Quota Forecasting",
    "landing.feat2Desc": "Predict month-end electricity bills and quota threshold dates in advance.",
    "landing.feat3Title": "Appliance Anomaly Detection",
    "landing.feat3Desc": "Instant alerts when an appliance exceeds normal operating thresholds.",
    "landing.feat4Title": "Smart Advisory Engine",
    "landing.feat4Desc": "Gemini-powered personalized energy efficiency recommendations sent to your inbox.",
    "landing.allInOne": "Everything to manage your energy in one place",

    // Footer
    "footer.copyright": "© 2026 VegaWatt",
    "footer.tagline": "Smart Home Energy Management & Telemetry Platform",
    "footer.team": "Developer Team:",

    // Common
    "common.justNow": "just now",
    "common.retry": "Retry",
    "common.cancel": "Cancel",
    "common.save": "Save",
    "common.add": "Add",
    "common.delete": "Delete",
    "common.loading": "Loading...",
    "common.close": "Close",
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

  function t(key: string, params?: Record<string, string | number>): string {
    let value = TRANSLATIONS[language][key] ?? TRANSLATIONS.tr[key] ?? key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        value = value.replace(new RegExp(`\\{${k}\\}`, "g"), String(v));
      }
    }
    return value;
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
