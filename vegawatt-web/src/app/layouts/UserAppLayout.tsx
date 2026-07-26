import { useState } from "react";
import { Bell, Cpu, FileCode, Home, LayoutDashboard, LogOut, Mail, Sparkles, User, Users } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { BrandMark } from "../../shared/components/BrandMark";
import { ThemeToggle } from "../../shared/components/ThemeToggle";
import { LanguageToggle } from "../../shared/components/LanguageToggle";
import { Footer } from "../../shared/components/Footer";
import { ProfileSettingsModal } from "../../features/user/ProfileSettingsModal";
import { AdminUserManagementModal } from "../../features/admin/AdminUserManagementModal";
import { useLiveHomesQuery } from "../../shared/hooks/useHomesQueries";
import { useLanguage } from "../../shared/i18n/LanguageContext";

function initialFromEmail(email: string | undefined): string {
  return email ? email[0]!.toUpperCase() : "?";
}

export function UserAppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { isError: isSystemDisconnected } = useLiveHomesQuery();
  const { t } = useLanguage();

  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [isAdminUserModalOpen, setIsAdminUserModalOpen] = useState(false);

  const navItems = [
    { to: "/app/overview", label: t("nav.overview"), icon: LayoutDashboard },
    { to: "/app/homes", label: t("nav.homes"), icon: Home },
    { to: "/app/devices", label: t("nav.devices"), icon: Cpu },
    { to: "/app/assistant", label: t("nav.assistant"), icon: Sparkles },
    { to: "/app/notifications", label: t("nav.notifications"), icon: Bell },
  ];

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  return (
    <div className="flex min-h-screen flex-col pb-16 sm:pb-0 w-full max-w-full overflow-x-hidden">
      {/* Unified single-row top navbar: brand, primary nav, and utilities all in one line */}
      <header className="sticky top-0 z-40 border-b border-border bg-topbar-bg/95 backdrop-blur-md w-full overflow-hidden">
        <div className="mx-auto flex max-w-[1600px] w-full min-w-0 items-center justify-between gap-2 sm:gap-4 px-3 sm:px-6 lg:px-8 py-2.5 sm:py-3">
          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            <BrandMark />
            <div className="hidden h-6 w-px bg-border md:block shrink-0" aria-hidden="true" />
          </div>

          <nav className="hidden min-w-0 items-center gap-1 md:flex overflow-x-auto py-0.5" aria-label="Ana gezinme">
            {navItems.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                title={label}
                className={({ isActive }) =>
                  `relative flex shrink-0 items-center gap-1.5 rounded-input px-2.5 py-1.5 text-xs xl:text-sm font-medium transition ${
                    isActive ? "bg-primary-soft text-primary" : "text-text-secondary hover:bg-surface-subtle hover:text-text-primary"
                  }`
                }
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
                <span className="hidden xl:inline">{label}</span>
              </NavLink>
            ))}
          </nav>

          <div className="flex shrink-0 items-center gap-1.5 sm:gap-2">
            {user?.role === "ADMIN" && (
              <div className="hidden items-center gap-1.5 border-r border-border pr-2 xl:flex">
                <button
                  onClick={() => setIsAdminUserModalOpen(true)}
                  className="flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-xs font-semibold text-amber-600 transition hover:bg-amber-500 hover:text-white dark:text-amber-400"
                  title={t("nav.userManagement")}
                >
                  <Users className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                  <span className="hidden 2xl:inline">{t("nav.userManagement")}</span>
                </button>
                <a
                  href="http://localhost:8080/swagger-ui/index.html"
                  target="_blank"
                  rel="noreferrer"
                  className="hidden 2xl:flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary-soft px-2.5 py-1 text-xs font-semibold text-primary transition hover:bg-primary hover:text-on-primary"
                  title="Swagger Core API Dokümantasyonu"
                >
                  <FileCode className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                  <span>Swagger UI</span>
                </a>
                <a
                  href="http://localhost:8025"
                  target="_blank"
                  rel="noreferrer"
                  className="hidden 2xl:flex items-center gap-1.5 rounded-full border border-border bg-surface px-2.5 py-1 text-xs font-semibold text-text-secondary transition hover:border-primary hover:text-primary"
                  title="Mailpit E-posta Test Paneli"
                >
                  <Mail className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                  <span>Mailpit</span>
                </a>
              </div>
            )}

            <span className="hidden items-center gap-1.5 text-xs text-text-secondary 2xl:flex">
              <span
                className={`h-1.5 w-1.5 rounded-full ${isSystemDisconnected ? "bg-danger" : "bg-success"}`}
                aria-hidden="true"
              />
              {isSystemDisconnected ? t("nav.systemDisconnected") : t("nav.systemLive")}
            </span>

            <LanguageToggle />
            <ThemeToggle />

            <div className="flex items-center gap-1.5 border-l border-border pl-2">
              <button
                onClick={() => setIsProfileModalOpen(true)}
                className="flex items-center gap-1.5 rounded-lg border border-border bg-surface px-2 py-1 text-xs font-medium text-text-primary transition hover:border-primary hover:bg-surface-subtle"
                title="Profil ve Hesap Ayarları"
              >
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary-soft text-xs font-bold text-primary">
                  {initialFromEmail(user?.email)}
                </span>
                <span className="hidden 2xl:inline max-w-[140px] truncate">{user?.email}</span>
                <User className="h-3.5 w-3.5 text-text-secondary shrink-0" />
              </button>

              <button
                type="button"
                onClick={handleLogout}
                className="flex items-center gap-1.5 rounded-lg border border-rose-500/20 bg-rose-500/10 px-2 py-1 text-xs font-semibold text-rose-600 transition hover:bg-rose-500 hover:text-white dark:text-rose-400"
                title={t("nav.logout")}
              >
                <LogOut className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                <span className="hidden 2xl:inline">{t("nav.logout")}</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="flex-1 w-full max-w-full overflow-x-hidden p-3 sm:p-5 lg:p-6">
        <Outlet />
      </main>

      <Footer />

      {/* Mobile Bottom Navigation */}
      <nav className="fixed bottom-0 left-0 right-0 z-50 flex border-t border-border bg-surface shadow-lg sm:hidden">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center justify-center py-2 text-[11px] font-medium transition ${
                isActive ? "text-primary" : "text-text-muted hover:text-text-primary"
              }`
            }
          >
            <Icon className="h-5 w-5 mb-0.5" aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Profile Settings Modal */}
      <ProfileSettingsModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
      />

      {/* Admin User Management Modal */}
      <AdminUserManagementModal
        isOpen={isAdminUserModalOpen}
        onClose={() => setIsAdminUserModalOpen(false)}
      />
    </div>
  );
}
