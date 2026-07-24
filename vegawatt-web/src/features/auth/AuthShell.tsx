import { CheckCircle2 } from "lucide-react";
import type { ReactNode } from "react";
import { BrandMark } from "../../shared/components/BrandMark";

interface AuthShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer: ReactNode;
}

const HIGHLIGHTS = [
  "Canlı tüketim verilerini saniyeler içinde izleyin",
  "Aylık bütçenizi önceden tahmin edin",
  "Cihaz bazlı akıllı tasarruf önerileri alın",
];

export function AuthShell({ title, subtitle, children, footer }: AuthShellProps) {
  return (
    <div className="grid min-h-screen grid-cols-1 lg:grid-cols-2">
      <div className="relative hidden flex-col justify-between overflow-hidden bg-primary-soft px-12 py-12 lg:flex">
        <div
          className="pointer-events-none absolute -right-24 -top-24 h-80 w-80 rounded-full bg-primary/20 blur-3xl"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -bottom-32 -left-16 h-72 w-72 rounded-full bg-energy-accent/20 blur-3xl"
          aria-hidden="true"
        />

        <div className="relative">
          <BrandMark />
        </div>

        <div className="relative max-w-sm">
          <h2 className="text-4xl font-bold leading-tight tracking-tight text-text-primary">
            Enerjiyi yalnız izlemeyin.
            <br />
            <span className="text-gradient-flow">Nereye gittiğini anlayın.</span>
          </h2>
          <p className="mt-3 text-sm text-text-secondary">
            Canlı tüketim, bütçe tahmini ve cihaz bazlı akıllı öneriler tek panelde.
          </p>
          <ul className="mt-8 flex flex-col gap-3">
            {HIGHLIGHTS.map((item) => (
              <li key={item} className="flex items-start gap-2.5 text-sm text-text-primary">
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden="true" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-xs text-text-muted">© {new Date().getFullYear()} VegaWatt. Tüm hakları saklıdır.</p>
      </div>

      <div className="flex flex-col justify-center px-6 py-12 sm:px-12 lg:px-16">
        <div className="mx-auto w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <BrandMark />
          </div>

          <h1 className="text-3xl font-semibold tracking-tight text-text-primary">{title}</h1>
          <p className="mt-2 text-base text-text-secondary">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <div className="mt-6 text-sm text-text-secondary">{footer}</div>
        </div>
      </div>
    </div>
  );
}
