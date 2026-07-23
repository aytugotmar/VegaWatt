import { Cpu, Home, LayoutDashboard, LogOut, Zap } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { ThemeToggle } from "../../shared/components/ThemeToggle";

const NAV_ITEMS = [
  { to: "/app/overview", label: "Genel Bakış", icon: LayoutDashboard },
  { to: "/app/homes", label: "Evlerim", icon: Home },
  { to: "/app/devices", label: "Cihazlarım", icon: Cpu },
];

function initialFromEmail(email: string | undefined): string {
  return email ? email[0]!.toUpperCase() : "?";
}

export function UserAppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-60 shrink-0 flex-col border-r border-border bg-sidebar-bg sm:flex">
        <div className="flex items-center gap-2 px-5 py-5">
          <span className="flex h-8 w-8 items-center justify-center rounded-input bg-primary text-white">
            <Zap className="h-4 w-4" aria-hidden="true" />
          </span>
          <span className="text-base font-semibold text-text-primary">VegaWatt</span>
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-3">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `relative flex h-11 items-center gap-2.5 rounded-input px-3 text-sm font-medium transition ${
                  isActive
                    ? "bg-primary-soft text-primary"
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

        <div className="flex items-center gap-2.5 border-t border-border p-4">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-subtle text-xs font-semibold text-text-secondary">
            {initialFromEmail(user?.email)}
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-text-primary">{user?.email}</p>
            <button
              type="button"
              onClick={handleLogout}
              className="flex items-center gap-1 text-xs font-medium text-text-secondary hover:text-danger"
            >
              <LogOut className="h-3 w-3" aria-hidden="true" />
              Çıkış Yap
            </button>
          </div>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-border bg-topbar-bg px-6 py-3">
          <span className="text-sm font-medium text-text-primary">Merhaba</span>
          <div className="flex items-center gap-3">
            <span className="hidden items-center gap-1.5 text-xs text-text-secondary sm:flex">
              <span className="h-1.5 w-1.5 rounded-full bg-success" aria-hidden="true" />
              Sistem canlı
            </span>
            <ThemeToggle />
          </div>
        </header>
        <main className="flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
