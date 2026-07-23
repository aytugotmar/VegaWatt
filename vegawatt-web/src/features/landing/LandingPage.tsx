import { AlertTriangle, Cpu, Gauge, LineChart, Sparkles, Wallet, Zap } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
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

const FEATURES = [
  {
    icon: Gauge,
    title: "Canlı tüketim takibi",
    description: "Her evin ve cihazın anlık gücü saniyeler içinde güncellenir.",
  },
  {
    icon: Wallet,
    title: "Bütçe ve kota tahmini",
    description: "Ay sonunda ne kadar ödeyeceğinizi ve kotayı ne zaman aşacağınızı önceden görün.",
  },
  {
    icon: AlertTriangle,
    title: "Cihaz anomali tespiti",
    description: "Güvenli sınırı aşan bir cihaz olduğunda anında haberdar olun.",
  },
  {
    icon: Sparkles,
    title: "Akıllı öneri motoru",
    description: "Gemini destekli, cihaza özel Türkçe tasarruf önerileri e-postanıza gelir.",
  },
];

type AuthTab = "login" | "register";

export function LandingPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<AuthTab>("login");

  return (
    <div className="min-h-screen">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6">
        <div className="flex items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-input bg-primary text-white">
            <Zap className="h-4 w-4" aria-hidden="true" />
          </span>
          <span className="text-lg font-semibold text-text-primary">VegaWatt</span>
        </div>
        <nav className="hidden items-center gap-6 text-sm font-medium text-text-secondary sm:flex">
          <a href="#nasil-calisir" className="hover:text-text-primary">
            Nasıl Çalışır
          </a>
          <a href="#ozellikler" className="hover:text-text-primary">
            Özellikler
          </a>
          <Link to="/login" className="hover:text-text-primary">
            Giriş Yap
          </Link>
        </nav>
      </header>

      <main className="mx-auto grid max-w-6xl grid-cols-1 gap-10 px-6 py-10 lg:grid-cols-2 lg:items-center lg:py-16">
        <div className="max-w-xl">
          <h1 className="text-4xl font-semibold leading-tight tracking-tight text-text-primary">
            Enerjiyi yalnız izlemeyin. Nereye gittiğini anlayın.
          </h1>
          <p className="mt-4 text-lg text-text-secondary">
            Canlı tüketim, bütçe tahmini ve cihaz bazlı akıllı öneriler tek panelde.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => setTab("register")}
              className="btn-primary h-11 px-5 text-sm"
            >
              Ücretsiz başla
            </button>
            <a href="#demo" className="btn-secondary h-11 px-5 text-sm">
              Demoyu incele
            </a>
          </div>
          <div className="mt-8 max-w-sm" id="demo">
            <DemoStatStrip />
          </div>
        </div>

        <div className="rounded-modal border border-border bg-surface-raised p-6 shadow-[var(--shadow-modal)] sm:p-8">
          <div className="mb-6 flex gap-1 rounded-input bg-surface-subtle p-1">
            <button
              type="button"
              onClick={() => setTab("login")}
              className={`flex-1 rounded-input py-2 text-sm font-medium transition ${
                tab === "login" ? "bg-surface text-text-primary shadow-sm" : "text-text-secondary"
              }`}
            >
              Giriş Yap
            </button>
            <button
              type="button"
              onClick={() => setTab("register")}
              className={`flex-1 rounded-input py-2 text-sm font-medium transition ${
                tab === "register" ? "bg-surface text-text-primary shadow-sm" : "text-text-secondary"
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

      <section id="nasil-calisir" className="mx-auto max-w-6xl px-6 py-16">
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-3">
          {STEPS.map(({ icon: Icon, title, description }) => (
            <div key={title} className="flex flex-col gap-2">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-soft text-primary">
                <Icon className="h-4 w-4" aria-hidden="true" />
              </span>
              <h2 className="text-base font-semibold text-text-primary">{title}</h2>
              <p className="text-sm text-text-secondary">{description}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="ozellikler" className="mx-auto max-w-6xl px-6 py-16">
        <h2 className="mb-8 text-2xl font-semibold text-text-primary">Özellikler</h2>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map(({ icon: Icon, title, description }) => (
            <div key={title} className="flex flex-col gap-2 rounded-input border border-border bg-surface p-4">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-soft text-primary">
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
