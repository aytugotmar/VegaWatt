import { Zap } from "lucide-react";
import type { ReactNode } from "react";
import { DemoStatStrip } from "../landing/DemoStatStrip";

interface AuthShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer: ReactNode;
}

export function AuthShell({ title, subtitle, children, footer }: AuthShellProps) {
  return (
    <div className="grid min-h-screen grid-cols-1 lg:grid-cols-2">
      <div className="hidden flex-col justify-between bg-primary-soft px-12 py-12 lg:flex">
        <div className="flex items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-input bg-primary text-white">
            <Zap className="h-4 w-4" aria-hidden="true" />
          </span>
          <span className="text-base font-semibold text-text-primary">VegaWatt</span>
        </div>
        <div className="max-w-sm">
          <h2 className="text-2xl font-semibold text-text-primary">
            Enerjiyi yalnız izlemeyin. Nereye gittiğini anlayın.
          </h2>
          <p className="mt-3 text-sm text-text-secondary">
            Canlı tüketim, bütçe tahmini ve cihaz bazlı akıllı öneriler tek panelde.
          </p>
        </div>
        <DemoStatStrip />
      </div>

      <div className="flex flex-col justify-center px-6 py-12 sm:px-12 lg:px-16">
        <div className="mx-auto w-full max-w-sm">
          <h1 className="text-2xl font-semibold text-text-primary">{title}</h1>
          <p className="mt-1.5 text-sm text-text-secondary">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <div className="mt-6 text-sm text-text-secondary">{footer}</div>
        </div>
      </div>
    </div>
  );
}
