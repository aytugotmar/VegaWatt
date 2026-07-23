import { lazy, Suspense, type ReactNode } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "../features/auth/AuthContext";
import { ErrorBoundary } from "../shared/components/ErrorBoundary";
import { Spinner } from "../shared/components/Skeleton";
import { AppProviders } from "./AppProviders";
import { UserAppLayout } from "./layouts/UserAppLayout";

const LandingPage = lazy(() => import("../features/landing/LandingPage").then((m) => ({ default: m.LandingPage })));
const LoginPage = lazy(() => import("../features/auth/LoginPage").then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import("../features/auth/RegisterPage").then((m) => ({ default: m.RegisterPage })));
const DashboardPage = lazy(() =>
  import("../features/dashboard/DashboardPage").then((m) => ({ default: m.DashboardPage })),
);
const HomeDetailsPage = lazy(() =>
  import("../features/home-details/HomeDetailsPage").then((m) => ({ default: m.HomeDetailsPage })),
);
const UserOverviewPage = lazy(() =>
  import("../features/overview/UserOverviewPage").then((m) => ({ default: m.UserOverviewPage })),
);
const DevicesPage = lazy(() => import("../features/devices/DevicesPage").then((m) => ({ default: m.DevicesPage })));
const DeviceDetailsPage = lazy(() =>
  import("../features/devices/DeviceDetailsPage").then((m) => ({ default: m.DeviceDetailsPage })),
);
const NotificationsPage = lazy(() =>
  import("../features/notifications/NotificationsPage").then((m) => ({ default: m.NotificationsPage })),
);

function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <Spinner label="Yükleniyor..." />
    </div>
  );
}

function ProtectedRoute({ children, requireRole }: { children: ReactNode; requireRole?: "ADMIN" }) {
  const { user, isInitializing } = useAuth();

  if (isInitializing) {
    return <FullPageSpinner />;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (requireRole && user.role !== requireRole) {
    return <Navigate to="/app/overview" replace />;
  }
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Suspense fallback={<FullPageSpinner />}>
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
          path="/app"
          element={
            <ProtectedRoute>
              <UserAppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="overview" replace />} />
          <Route
            path="overview"
            element={
              <ErrorBoundary>
                <UserOverviewPage />
              </ErrorBoundary>
            }
          />
          <Route
            path="homes"
            element={
              <ErrorBoundary>
                <DashboardPage mode="USER" />
              </ErrorBoundary>
            }
          />
          <Route
            path="homes/:homeId"
            element={
              <ErrorBoundary>
                <HomeDetailsPage />
              </ErrorBoundary>
            }
          />
          <Route
            path="devices"
            element={
              <ErrorBoundary>
                <DevicesPage />
              </ErrorBoundary>
            }
          />
          <Route
            path="devices/:applianceId"
            element={
              <ErrorBoundary>
                <DeviceDetailsPage />
              </ErrorBoundary>
            }
          />
          <Route
            path="notifications"
            element={
              <ErrorBoundary>
                <NotificationsPage />
              </ErrorBoundary>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
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
