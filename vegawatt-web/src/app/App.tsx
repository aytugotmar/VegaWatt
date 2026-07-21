import type { ReactNode } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "../features/auth/AuthContext";
import { LoginPage } from "../features/auth/LoginPage";
import { RegisterPage } from "../features/auth/RegisterPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { HomeDetailsPage } from "../features/home-details/HomeDetailsPage";
import { LandingPage } from "../features/landing/LandingPage";
import { ErrorBoundary } from "../shared/components/ErrorBoundary";
import { Spinner } from "../shared/components/Skeleton";
import { AppProviders } from "./AppProviders";

function ProtectedRoute({ children, requireRole }: { children: ReactNode; requireRole?: "ADMIN" }) {
  const { user, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner label="Yükleniyor..." />
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (requireRole && user.role !== requireRole) {
    return <Navigate to="/app/homes" replace />;
  }
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/admin/operations"
        element={
          <ProtectedRoute requireRole="ADMIN">
            <ErrorBoundary>
              <DashboardPage mode="ADMIN" />
            </ErrorBoundary>
          </ProtectedRoute>
        }
      />
      <Route
        path="/app/homes"
        element={
          <ProtectedRoute>
            <ErrorBoundary>
              <DashboardPage mode="USER" />
            </ErrorBoundary>
          </ProtectedRoute>
        }
      />
      <Route
        path="/app/homes/:homeId"
        element={
          <ProtectedRoute>
            <ErrorBoundary>
              <HomeDetailsPage />
            </ErrorBoundary>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export function App() {
  return (
    <BrowserRouter>
      <AppProviders>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </AppProviders>
    </BrowserRouter>
  );
}
