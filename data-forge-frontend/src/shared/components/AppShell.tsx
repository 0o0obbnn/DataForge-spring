import { BarChart3, Boxes, DatabaseZap, FileStack, LayoutDashboard, ListChecks, LogOut } from "lucide-react";
import { useTranslation } from "react-i18next";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useAuthStore } from "@/features/auth/authStore";
import { LanguageSwitch } from "@/shared/components/LanguageSwitch";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/lib/utils";

export function AppShell() {
  const navigate = useNavigate();
  const { t } = useTranslation("common");
  const username = useAuthStore((state) => state.username);
  const isMockMode = useAuthStore((state) => state.isMockMode);
  const logout = useAuthStore((state) => state.logout);

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  const navigationItems = [
    { label: t("nav.dashboard"), href: "/dashboard", icon: LayoutDashboard },
    { label: t("nav.builder"), href: "/builder", icon: DatabaseZap },
    { label: t("nav.catalog"), href: "/catalog", icon: Boxes },
    { label: t("nav.tasks"), href: "/tasks", icon: ListChecks },
    { label: t("nav.templates"), href: "/templates", icon: FileStack },
  ];

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100">
      <div className="relative grid min-h-screen grid-cols-[16rem_1fr]">
        <aside className="border-r border-slate-700 bg-slate-900 px-4 py-5">
          <div className="mb-8 flex items-center gap-3 px-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-blue-600 text-white">
              <BarChart3 className="size-5" aria-hidden="true" />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-100">DataForge</p>
              <p className="text-xs text-slate-400">{t("nav.dashboard")}</p>
            </div>
          </div>

          <nav className="space-y-1" aria-label="Primary navigation">
            {navigationItems.map((item) => (
              <NavLink
                key={item.href}
                to={item.href}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition",
                    isActive
                      ? "bg-blue-600/10 text-blue-400"
                      : "text-slate-400 hover:bg-slate-800 hover:text-slate-200",
                  )
                }
              >
                <item.icon className="size-4" aria-hidden="true" />
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <div className="flex min-w-0 flex-col bg-slate-950">
          <header className="flex items-center justify-between border-b border-slate-800 bg-slate-950 px-6 py-4">
            <div>
              <h2 className="text-lg font-medium text-slate-100">DataForge</h2>
            </div>

            <div className="flex items-center gap-3">
              <LanguageSwitch />
              <StatusBadge status={isMockMode ? "warning" : "healthy"}>
                {isMockMode ? t("status.devMock") : t("status.jwt")}
              </StatusBadge>
              <span className="text-sm text-slate-400">{username ?? t("appShell.userDefault")}</span>
              <Button variant="outline" size="sm" onClick={handleLogout}>
                <LogOut className="mr-1 size-4" aria-hidden="true" />
                {t("auth.logout")}
              </Button>
            </div>
          </header>

          <main className="min-w-0 flex-1 px-6 py-6">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
