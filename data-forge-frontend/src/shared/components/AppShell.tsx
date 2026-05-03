import { BarChart3, Boxes, DatabaseZap, FileStack, LayoutDashboard, ListChecks, LogOut } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useAuthStore } from "@/features/auth/authStore";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/lib/utils";

const navigationItems = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Builder", href: "/builder", icon: DatabaseZap },
  { label: "Catalog", href: "/catalog", icon: Boxes },
  { label: "Tasks", href: "/tasks", icon: ListChecks },
  { label: "Templates", href: "/templates", icon: FileStack },
];

export function AppShell() {
  const navigate = useNavigate();
  const username = useAuthStore((state) => state.username);
  const isMockMode = useAuthStore((state) => state.isMockMode);
  const logout = useAuthStore((state) => state.logout);

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-[#050816] text-slate-50">
      <div className="pointer-events-none fixed inset-0 bg-[linear-gradient(rgba(34,211,238,0.06)_1px,transparent_1px),linear-gradient(90deg,rgba(34,211,238,0.06)_1px,transparent_1px)] bg-[size:44px_44px]" />
      <div className="relative grid min-h-screen grid-cols-[18rem_1fr]">
        <aside className="border-r border-slate-700/40 bg-slate-950/70 px-5 py-6 backdrop-blur-xl">
          <div className="mb-10 flex items-center gap-3">
            <div className="flex size-11 items-center justify-center rounded-2xl border border-cyan-300/30 bg-cyan-300/10 text-cyan-200 shadow-[0_0_30px_rgba(34,211,238,0.18)]">
              <BarChart3 className="size-5" aria-hidden="true" />
            </div>
            <div>
              <p className="text-sm uppercase tracking-[0.32em] text-cyan-200/80">DataForge</p>
              <h1 className="text-lg font-semibold text-slate-50">Console</h1>
            </div>
          </div>

          <nav className="space-y-2" aria-label="Primary navigation">
            {navigationItems.map((item) => (
              <NavLink
                key={item.href}
                to={item.href}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-2xl border px-3 py-3 text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-300",
                    isActive
                      ? "border-cyan-300/40 bg-cyan-300/10 text-cyan-100"
                      : "border-transparent text-slate-400 hover:border-slate-700/70 hover:bg-slate-900/70 hover:text-slate-100",
                  )
                }
              >
                <item.icon className="size-4" aria-hidden="true" />
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <div className="flex min-w-0 flex-col">
          <header className="flex items-center justify-between border-b border-slate-700/40 bg-slate-950/50 px-8 py-5 backdrop-blur-xl">
            <div>
              <p className="text-xs uppercase tracking-[0.28em] text-slate-500">Developer Operations</p>
              <h2 className="mt-1 text-2xl font-semibold text-slate-50">DataForge Console</h2>
            </div>

            <div className="flex items-center gap-3">
              <StatusBadge status={isMockMode ? "warning" : "healthy"}>
                {isMockMode ? "DEV MOCK" : "JWT"}
              </StatusBadge>
              <span className="rounded-full border border-slate-700/60 px-3 py-1 text-sm text-slate-300">
                {username ?? "operator"}
              </span>
              <Button variant="outline" size="sm" onClick={handleLogout}>
                <LogOut className="size-4" aria-hidden="true" />
                Logout
              </Button>
            </div>
          </header>

          <main className="min-w-0 flex-1 px-8 py-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
