import { AlertOctagon } from "lucide-react";
import { Component, type ReactNode } from "react";
import { Button } from "./Button";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    console.error("Uygulama beklenmeyen bir hatayla karşılaştı:", error);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-app-bg p-6 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-danger-soft text-danger">
          <AlertOctagon className="h-6 w-6" aria-hidden="true" />
        </div>
        <div>
          <p className="text-base font-semibold text-text-primary">Beklenmeyen bir hata oluştu</p>
          <p className="mt-1 text-sm text-text-secondary">
            Sayfayı yenileyerek tekrar deneyebilirsiniz. Sorun devam ederse lütfen daha sonra tekrar deneyin.
          </p>
        </div>
        <Button variant="primary" onClick={() => window.location.reload()}>
          Sayfayı Yenile
        </Button>
      </div>
    );
  }
}
