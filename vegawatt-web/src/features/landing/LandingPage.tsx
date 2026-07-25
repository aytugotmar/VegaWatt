import { AlertTriangle, Cpu, Gauge, LineChart, Sparkles, Wallet } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { BrandMark } from "../../shared/components/BrandMark";
import { TONE_BADGE_CLASSES, type Tone } from "../../shared/utils/toneClasses";
import { LoginForm } from "../auth/LoginForm";
import { RegisterForm } from "../auth/RegisterForm";
import { DemoStatStrip } from "./DemoStatStrip";

const STEPS = [
  {
    icon: Cpu,
    title: "Cihazlar veri üretir",
    description: "Evinizdeki her cihazın anlık güç tüketimi sürekli ölçülür.",
  },
  {
    icon: LineChart,
    title: "VegaWatt analiz eder",
    description: "Tüketim geçmişi, bütçe hedefleri ve anomaliler otomatik değerlendirilir.",
  },
  {
    icon: Sparkles,
    title: "Siz aksiyon alırsınız",
    description: "Kişiselleştirilmiş önerilerle bütçenizi aşmadan tasarruf edersiniz.",
  },
];

const FEATURES: { icon: typeof Gauge; title: string; description: string; tone: Tone }[] = [
  {
    icon: Gauge,
    title: "Canlı tüketim takibi",
    description: "Her evin ve cihazın anlık gücü saniyeler içinde güncellenir.",
    tone: "primary",
  },
  {
    icon: Wallet,
    title: "Bütçe ve kota tahmini",
    description: "Ay sonunda ne kadar ödeyeceğinizi ve kotayı ne zaman aşacağınızı önceden görün.",
    tone: "warning",
  },
  {
    icon: AlertTriangle,
    title: "Cihaz anomali tespiti",
    description: "Güvenli sınırı aşan bir cihaz olduğunda anında haberdar olun.",
    tone: "danger",
  },
  {
    icon: Sparkles,
    title: "Akıllı öneri motoru",
    description: "Gemini destekli, cihaza özel Türkçe tasarruf önerileri e-postanıza gelir.",
    tone: "accent",
  },
];

type AuthTab = "login" | "register";

export function LandingPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<AuthTab>("login");

  return (
    <div className="min-h-screen w-full max-w-full overflow-x-hidden">
      <header className="mx-auto flex max-w-6xl items-center px-4 sm:px-6 py-4 sm:py-6">
        <BrandMark />
      </header>

      <main className="relative mx-auto grid max-w-6xl grid-cols-1 gap-10 overflow-hidden px-4 sm:px-6 py-6 sm:py-10 lg:grid-cols-2 lg:items-center lg:py-16">
        <div
          className="pointer-events-none absolute -right-32 -top-24 h-96 w-96 rounded-full bg-primary/20 blur-3xl"
          aria-hidden="true"
        />
        <div className="relative max-w-xl">
          <h1 className="text-4xl font-bold leading-tight tracking-tight text-text-primary sm:text-5xl">
            Enerjiyi yalnız izlemeyin.
            <br />
            <span className="text-gradient-flow">Nereye gittiğini anlayın.</span>
          </h1>
          <p className="mt-4 text-lg text-text-secondary">
            Canlı tüketim, bütçe tahmini ve cihaz bazlı akıllı öneriler tek panelde.
          </p>
          <div className="mt-8 max-w-sm" id="demo">
            <DemoStatStrip />
          </div>
        </div>

        <div className="rounded-modal border border-border bg-surface-raised p-6 shadow-[var(--shadow-modal)] sm:p-8">
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
              Giriş Yap
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
              Hesap Oluştur
            </button>
          </div>

          {tab === "login" ? (
            <LoginForm onSuccess={() => navigate("/app/overview")} />
          ) : (
            <RegisterForm onSuccess={() => navigate("/app/overview")} />
          )}

          <p className="mt-4 text-xs text-text-muted">
            Verileriniz yalnızca size ait evlerle sınırlandırılır.
          </p>
        </div>
      </main>

      <section id="nasil-calisir" className="mx-auto max-w-6xl px-4 sm:px-6 py-10 sm:py-16">
        <div className="mb-10 max-w-lg">
          <p className="text-xs font-semibold uppercase tracking-wide text-primary">Nasıl Çalışır</p>
          <h2 className="mt-1 text-2xl font-bold tracking-tight text-text-primary sm:text-3xl">
            Üç adımda tam kontrol
          </h2>
        </div>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          {STEPS.map(({ icon: Icon, title, description }, index) => (
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
          <p className="text-xs font-semibold uppercase tracking-wide text-primary">Özellikler</p>
          <h2 className="mt-1 text-2xl font-bold tracking-tight text-text-primary sm:text-3xl">
            Enerjinizi yöneten her şey tek yerde
          </h2>
        </div>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map(({ icon: Icon, title, description, tone }) => (
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
      </section>
    </div>
  );
}
