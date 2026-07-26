import { AlertTriangle, ChevronDown, Cpu, Gauge, LineChart, Sparkles, Wallet } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { BrandMark } from "../../shared/components/BrandMark";
import { LanguageToggle } from "../../shared/components/LanguageToggle";
import { ThemeToggle } from "../../shared/components/ThemeToggle";
import { Footer } from "../../shared/components/Footer";
import { useLanguage } from "../../shared/i18n/LanguageContext";
import { TONE_BADGE_CLASSES, type Tone } from "../../shared/utils/toneClasses";
import { LoginForm } from "../auth/LoginForm";
import { RegisterForm } from "../auth/RegisterForm";
import { DemoStatStrip } from "./DemoStatStrip";

type AuthTab = "login" | "register";

export function LandingPage() {
  const { t } = useLanguage();
  const navigate = useNavigate();
  const [tab, setTab] = useState<AuthTab>("login");

  const steps = [
    {
      icon: Cpu,
      title: t("landing.step1Title"),
      description: t("landing.step1Desc"),
    },
    {
      icon: LineChart,
      title: t("landing.step2Title"),
      description: t("landing.step2Desc"),
    },
    {
      icon: Sparkles,
      title: t("landing.step3Title"),
      description: t("landing.step3Desc"),
    },
  ];

  const features: { icon: typeof Gauge; title: string; description: string; tone: Tone }[] = [
    {
      icon: Gauge,
      title: t("landing.feat1Title"),
      description: t("landing.feat1Desc"),
      tone: "primary",
    },
    {
      icon: Wallet,
      title: t("landing.feat2Title"),
      description: t("landing.feat2Desc"),
      tone: "warning",
    },
    {
      icon: AlertTriangle,
      title: t("landing.feat3Title"),
      description: t("landing.feat3Desc"),
      tone: "danger",
    },
    {
      icon: Sparkles,
      title: t("landing.feat4Title"),
      description: t("landing.feat4Desc"),
      tone: "accent",
    },
  ];

  return (
    <div className="min-h-screen w-full max-w-full overflow-x-hidden bg-background">
      {/* Top Header with Language & Theme Toggles */}
      <header className="relative z-20 mx-auto flex max-w-6xl items-center justify-between px-4 sm:px-6 py-4">
        <BrandMark to="/" />
        <div className="flex items-center gap-2">
          <LanguageToggle />
          <ThemeToggle />
        </div>
      </header>

      {/* Hero Showcase Section with Animated Background GIF */}
      <section className="relative flex min-h-[calc(100vh-5rem)] flex-col justify-between overflow-hidden px-4 sm:px-6 py-4 sm:py-8">
        {/* Animated Background GIF with Dark Glass Vignette */}
        <div className="absolute inset-0 z-0 overflow-hidden">
          <img
            src="/assets/dashboard.gif"
            alt="VegaWatt Ambient Background"
            className="h-full w-full object-cover opacity-25 scale-105 filter blur-[2px] transition duration-1000"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-background/80 via-background/60 to-background" />
        </div>

        {/* Ambient Glow Orbs */}
        <div
          className="pointer-events-none absolute -left-32 top-1/4 z-0 h-96 w-96 rounded-full bg-primary/20 blur-3xl"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -right-32 bottom-1/4 z-0 h-96 w-96 rounded-full bg-accent/15 blur-3xl"
          aria-hidden="true"
        />

        {/* Hero Card Container (Glassmorphism Overlay) */}
        <div className="relative z-10 mx-auto grid max-w-6xl flex-1 grid-cols-1 items-center gap-8 py-4 lg:grid-cols-12 lg:gap-12">
          {/* Left Title & Tagline Box */}
          <div className="lg:col-span-7 flex flex-col justify-center">
            <div className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary-soft/80 px-3.5 py-1.5 text-xs font-semibold text-primary backdrop-blur-md w-fit mb-4">
              <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
              <span>{t("landing.heroBadge")}</span>
            </div>
            <h1 className="text-3xl font-extrabold leading-tight tracking-tight text-text-primary sm:text-5xl lg:text-6xl">
              {t("landing.heroTitle1")}
              <br />
              <span className="text-gradient-flow">{t("landing.heroTitle2")}</span>
            </h1>
            <p className="mt-4 max-w-xl text-base sm:text-lg text-text-secondary">
              {t("landing.heroSubtitle")}
            </p>

            {/* Quick Feature Badges */}
            <div className="mt-6 flex flex-wrap gap-3 text-xs font-medium text-text-secondary">
              <div className="flex items-center gap-2 rounded-full border border-border/80 bg-surface/80 px-3 py-1.5 backdrop-blur-md shadow-sm">
                <span className="h-2 w-2 rounded-full bg-success animate-pulse" />
                <span>{t("landing.liveTelemetry")}</span>
              </div>
              <div className="flex items-center gap-2 rounded-full border border-border/80 bg-surface/80 px-3 py-1.5 backdrop-blur-md shadow-sm">
                <span className="h-2 w-2 rounded-full bg-primary" />
                <span>{t("landing.aiAdvice")}</span>
              </div>
              <div className="flex items-center gap-2 rounded-full border border-border/80 bg-surface/80 px-3 py-1.5 backdrop-blur-md shadow-sm">
                <span className="h-2 w-2 rounded-full bg-warning" />
                <span>{t("landing.anomalyAlert")}</span>
              </div>
            </div>
          </div>

          {/* Right Login / Register Glass Card */}
          <div className="lg:col-span-5 relative">
            <div className="rounded-modal border border-border/80 bg-surface/90 p-6 shadow-2xl backdrop-blur-xl sm:p-8 transition duration-500 hover:border-primary/40">
              <div className="mb-6 flex gap-1 rounded-input bg-surface-subtle p-1">
                <button
                  type="button"
                  onClick={() => setTab("login")}
                  className={`flex-1 rounded-input py-2 text-sm font-semibold transition ${
                    tab === "login"
                      ? "bg-primary text-on-primary shadow-sm"
                      : "text-text-secondary hover:text-text-primary"
                  }`}
                >
                  {t("landing.login")}
                </button>
                <button
                  type="button"
                  onClick={() => setTab("register")}
                  className={`flex-1 rounded-input py-2 text-sm font-semibold transition ${
                    tab === "register"
                      ? "bg-primary text-on-primary shadow-sm"
                      : "text-text-secondary hover:text-text-primary"
                  }`}
                >
                  {t("landing.register")}
                </button>
              </div>

              {tab === "login" ? (
                <LoginForm onSuccess={() => navigate("/app/overview")} />
              ) : (
                <RegisterForm onSuccess={() => navigate("/app/overview")} />
              )}

              <p className="mt-4 text-xs text-text-muted text-center">
                {t("auth.privacyNote")}
              </p>
            </div>
          </div>
        </div>

        {/* Scroll Down Indicator Button */}
        <div className="relative z-10 flex flex-col items-center justify-center pt-4 pb-2">
          <button
            type="button"
            onClick={() => {
              document.getElementById("nasil-calisir")?.scrollIntoView({ behavior: "smooth" });
            }}
            className="group flex flex-col items-center gap-1.5 text-xs font-semibold text-text-muted transition hover:text-primary cursor-pointer focus:outline-none"
          >
            <span>{t("landing.scrollDown")}</span>
            <span className="flex h-8 w-8 items-center justify-center rounded-full border border-border/80 bg-surface/80 backdrop-blur-md transition group-hover:border-primary group-hover:bg-primary-soft group-hover:scale-110">
              <ChevronDown className="h-4 w-4 animate-bounce text-primary" aria-hidden="true" />
            </span>
          </button>
        </div>
      </section>

      {/* Content Below Hero Scroll */}
      <div className="border-t border-border/60 bg-surface-subtle/30">
        <section id="nasil-calisir" className="mx-auto max-w-6xl px-4 sm:px-6 py-12 sm:py-20">
          <div className="mb-10 max-w-lg">
            <p className="text-xs font-semibold uppercase tracking-wide text-primary">{t("landing.howItWorks")}</p>
            <h2 className="mt-1 text-2xl font-bold tracking-tight text-text-primary sm:text-3xl">
              {t("landing.threeSteps")}
            </h2>
          </div>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
            {steps.map(({ icon: Icon, title, description }, index) => (
              <div
                key={title}
                className="flex flex-col gap-3 rounded-input border border-border bg-surface p-5 transition hover:shadow-[var(--shadow-card-hover)]"
              >
                <div className="flex items-center justify-between">
                  <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary-soft text-primary">
                    <Icon className="h-4 w-4" aria-hidden="true" />
                  </span>
                  <span className="text-2xl font-bold text-border-strong" aria-hidden="true">
                    0{index + 1}
                  </span>
                </div>
                <h3 className="text-base font-semibold text-text-primary">{title}</h3>
                <p className="text-sm text-text-secondary">{description}</p>
              </div>
            ))}
          </div>
        </section>

        <section id="ozellikler" className="mx-auto max-w-6xl px-4 sm:px-6 py-10 sm:py-16">
          <div className="mb-8 max-w-lg">
            <p className="text-xs font-semibold uppercase tracking-wide text-primary">{t("landing.features")}</p>
            <h2 className="mt-1 text-2xl font-bold tracking-tight text-text-primary sm:text-3xl">
              {t("landing.allInOne")}
            </h2>
          </div>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {features.map(({ icon: Icon, title, description, tone }) => (
              <div
                key={title}
                className="flex flex-col gap-3 rounded-input border border-border bg-surface p-4 transition hover:shadow-[var(--shadow-card-hover)]"
              >
                <span className={`flex h-9 w-9 items-center justify-center rounded-full ${TONE_BADGE_CLASSES[tone]}`}>
                  <Icon className="h-4 w-4" aria-hidden="true" />
                </span>
                <h3 className="text-sm font-semibold text-text-primary">{title}</h3>
                <p className="text-sm text-text-secondary">{description}</p>
              </div>
            ))}
          </div>

          <div className="mt-12 max-w-xl mx-auto" id="demo">
            <DemoStatStrip />
          </div>
        </section>
      </div>

      <Footer />
    </div>
  );
}
