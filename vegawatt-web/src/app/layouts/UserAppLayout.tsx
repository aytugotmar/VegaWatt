import { useState } from "react";
import { Bell, Cpu, FileCode, Home, LayoutDashboard, LogOut, Mail, User, Users, Zap } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { getAccessToken } from "../../shared/auth/tokenProvider";
import { ThemeToggle } from "../../shared/components/ThemeToggle";
import { ProfileSettingsModal } from "../../features/user/ProfileSettingsModal";
import { AdminUserManagementModal } from "../../features/admin/AdminUserManagementModal";

const NAV_ITEMS = [
  { to: "/app/overview", label: "Genel Bakış", icon: LayoutDashboard },
  { to: "/app/homes", label: "Evlerim", icon: Home },
  { to: "/app/devices", label: "Cihazlarım", icon: Cpu },
  { to: "/app/notifications", label: "Bildirimler", icon: Bell },
];

function initialFromEmail(email: string | undefined): string {
  return email ? email[0]!.toUpperCase() : "?";
}

export function UserAppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [isAdminUserModalOpen, setIsAdminUserModalOpen] = useState(false);

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  return (
    <div className="flex min-h-screen pb-16 sm:pb-0">
      {/* Fixed Sticky Sidebar - Does not scroll with content */}
      <aside className="hidden h-screen sticky top-0 w-60 shrink-0 flex-col border-r border-border bg-sidebar-bg sm:flex z-30">
        <div className="flex items-center gap-2 px-5 py-5">
          <span className="flex h-8 w-8 items-center justify-center rounded-input bg-primary text-white shadow-md">
            <Zap className="h-4 w-4" aria-hidden="true" />
          </span>
          <span className="text-base font-bold tracking-tight text-text-primary">VegaWatt</span>
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-3 py-2">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `relative flex h-11 items-center gap-2.5 rounded-input px-3 text-sm font-medium transition ${
                  isActive
                    ? "bg-primary-soft text-primary font-semibold"
                    : "text-text-secondary hover:bg-surface-subtle hover:text-text-primary"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  {isActive && (
                    <span className="absolute inset-y-1.5 left-0 w-[3px] rounded-full bg-primary" aria-hidden="true" />
                  )}
                  <Icon className="h-[18px] w-[18px]" aria-hidden="true" />
                  {label}
                </>
              )}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Topbar Header */}
        <header className="sticky top-0 z-40 flex items-center justify-between border-b border-border bg-topbar-bg/95 px-6 py-2.5 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <span className="text-sm font-semibold text-text-primary">Merhaba, {user?.email?.split('@')[0]}</span>
            {user?.role === "ADMIN" && (
              <div className="flex items-center gap-2 border-l border-border pl-3">
                <button
                  onClick={() => setIsAdminUserModalOpen(true)}
                  className="flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-xs font-semibold text-amber-600 transition hover:bg-amber-500 hover:text-white dark:text-amber-400"
                  title="Kullanıcı & Yetki Yönetimi Paneli"
                >
                  <Users className="h-3.5 w-3.5" aria-hidden="true" />
                  Kullanıcı Yönetimi
                </button>
                <a
                  href={`http://localhost:8080/swagger-ui/index.html?url=/v3/api-docs?token=${getAccessToken() ?? ""}`}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary-soft px-3 py-1 text-xs font-semibold text-primary transition hover:bg-primary hover:text-white"
                  title="Swagger Core API Dokümantasyonu (Admin Yetkili)"
                >
                  <FileCode className="h-3.5 w-3.5" aria-hidden="true" />
                  Swagger UI
                </a>
                <a
                  href="http://localhost:8025"
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-1.5 rounded-full border border-border bg-surface px-3 py-1 text-xs font-semibold text-text-secondary transition hover:border-primary hover:text-primary"
                  title="Mailpit E-posta Test Paneli"
                >
                  <Mail className="h-3.5 w-3.5" aria-hidden="true" />
                  Mailpit
                </a>
              </div>
            )}
          </div>

          <div className="flex items-center gap-3">
            <span className="hidden items-center gap-1.5 text-xs text-text-secondary sm:flex">
              <span className="h-1.5 w-1.5 rounded-full bg-success" aria-hidden="true" />
              Sistem canlı
            </span>

            <ThemeToggle />

            {/* User Profile & Account Settings in Topbar */}
            <div className="flex items-center gap-2 border-l border-border pl-3">
              <button
                onClick={() => setIsProfileModalOpen(true)}
                className="flex items-center gap-2 rounded-lg border border-border bg-surface px-2.5 py-1 text-xs font-medium text-text-primary transition hover:border-primary hover:bg-surface-subtle"
                title="Profil ve Hesap Ayarları"
              >
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/20 text-xs font-bold text-primary">
                  {initialFromEmail(user?.email)}
                </span>
                <span className="hidden md:inline max-w-[140px] truncate">{user?.email}</span>
                <User className="h-3.5 w-3.5 text-text-secondary" />
              </button>

              <button
                type="button"
                onClick={handleLogout}
                className="flex items-center gap-1.5 rounded-lg border border-rose-500/20 bg-rose-500/10 px-3 py-1 text-xs font-semibold text-rose-600 transition hover:bg-rose-500 hover:text-white dark:text-rose-400"
                title="Çıkış Yap"
              >
                <LogOut className="h-3.5 w-3.5" aria-hidden="true" />
                <span className="hidden sm:inline">Çıkış</span>
              </button>
            </div>
          </div>
        </header>

        <main className="flex-1 p-4 sm:p-6">
          <Outlet />
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <nav className="fixed bottom-0 left-0 right-0 z-50 flex border-t border-border bg-surface shadow-lg sm:hidden">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
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
