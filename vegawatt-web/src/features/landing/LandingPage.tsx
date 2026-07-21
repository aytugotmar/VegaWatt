import { Activity, Bell, Gauge } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "../../shared/components/Button";

const FEATURES = [
  {
    icon: Gauge,
    title: "Canlı tüketim takibi",
    description: "Evinizin anlık enerji ve fatura durumunu tek ekrandan izleyin.",
  },
  {
    icon: Bell,
    title: "Anomali uyarıları",
    description: "Cihazlarınızda beklenmedik tüketim olduğunda anında haberdar olun.",
  },
  {
    icon: Activity,
    title: "Akıllı öneriler",
    description: "Tüketim geçmişinize göre kişiselleştirilmiş tasarruf önerileri alın.",
  },
];

export function LandingPage() {
  return (
    <div className="min-h-screen">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6">
        <span className="text-lg font-semibold text-text-primary">VegaWatt</span>
        <nav className="flex items-center gap-3">
          <Link to="/login" className="text-sm font-medium text-text-secondary hover:text-text-primary">
            Giriş yap
          </Link>
          <Link to="/register">
            <Button>Ücretsiz başla</Button>
          </Link>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-16">
        <div className="max-w-2xl">
          <h1 className="text-4xl font-semibold tracking-tight text-text-primary">
            Evinizin enerji tüketimini akıllıca yönetin
          </h1>
          <p className="mt-4 text-lg text-text-secondary">
            VegaWatt, evinizdeki cihazların enerji tüketimini gerçek zamanlı izler, anormal durumları tespit eder ve
            tasarruf etmenize yardımcı olur.
          </p>
          <div className="mt-8 flex gap-3">
            <Link to="/register">
              <Button>Hemen kayıt ol</Button>
            </Link>
            <Link to="/login">
              <Button variant="secondary">Giriş yap</Button>
            </Link>
          </div>
        </div>

        <div className="mt-16 grid grid-cols-1 gap-6 sm:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, description }) => (
            <div key={title} className="rounded-input border border-border bg-surface-raised p-5">
              <Icon className="h-6 w-6 text-primary" aria-hidden="true" />
              <h2 className="mt-3 text-base font-semibold text-text-primary">{title}</h2>
              <p className="mt-1 text-sm text-text-secondary">{description}</p>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
