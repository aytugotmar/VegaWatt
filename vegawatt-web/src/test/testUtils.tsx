import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";
import { ToastProvider } from "../shared/components/ToastProvider";
import { LanguageProvider } from "../shared/i18n/LanguageContext";

export function renderWithProviders(ui: ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchOnWindowFocus: false },
      mutations: { retry: false },
    },
  });

  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <LanguageProvider>
          <ToastProvider>{ui}</ToastProvider>
        </LanguageProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}
