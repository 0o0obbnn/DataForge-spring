import { ArrowRight, Boxes, DatabaseZap, ListChecks } from "lucide-react";
import { Link } from "react-router-dom";

import { DashboardStats } from "@/features/dashboard/DashboardStats";
import { deriveDashboardStats, useDashboardTasksQuery } from "@/features/dashboard/dashboardQueries";
import { TaskTable } from "@/features/tasks/TaskTable";
import { EmptyState } from "@/shared/components/EmptyState";
import { ErrorState } from "@/shared/components/ErrorState";
import { LoadingState } from "@/shared/components/LoadingState";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent } from "@/shared/components/ui/card";

const quickActions = [
  { label: "New Generation", href: "/builder", icon: DatabaseZap },
  { label: "Browse Catalog", href: "/catalog", icon: Boxes },
  { label: "Monitor Tasks", href: "/tasks", icon: ListChecks },
];

export function DashboardPage() {
  const tasksQuery = useDashboardTasksQuery();
  const tasks = tasksQuery.data ?? [];
  const stats = deriveDashboardStats(tasks);

  return (
    <div className="space-y-8">
      <section className="flex items-start justify-between gap-6">
        <div>
          <StatusBadge status="healthy">API READY</StatusBadge>
          <h1 className="mt-4 text-4xl font-semibold tracking-tight">Operations Dashboard</h1>
          <p className="mt-3 max-w-2xl text-slate-400">
            Track recent generation work, backend connectivity, and fast paths into the core console flows.
          </p>
        </div>
      </section>

      <DashboardStats stats={stats} />

      <section className="grid gap-4 md:grid-cols-3">
        {quickActions.map((action) => (
          <Card key={action.href} className="border-cyan-300/10 bg-slate-950/60 text-slate-50">
            <CardContent className="flex items-center justify-between gap-4 p-5">
              <div className="flex items-center gap-3">
                <action.icon className="size-5 text-cyan-200" aria-hidden="true" />
                <span className="font-medium">{action.label}</span>
              </div>
              <Button asChild variant="ghost" size="icon-sm">
                <Link to={action.href} aria-label={action.label}>
                  <ArrowRight className="size-4" aria-hidden="true" />
                </Link>
              </Button>
            </CardContent>
          </Card>
        ))}
      </section>

      <section className="space-y-4">
        <div>
          <h2 className="text-xl font-semibold text-slate-50">Recent Tasks</h2>
          <p className="mt-1 text-sm text-slate-500">Latest async and sync generation history from the backend.</p>
        </div>
        {tasksQuery.isPending ? <LoadingState label="Loading dashboard tasks" /> : null}
        {tasksQuery.isError ? (
          <ErrorState
            title="Unable to load dashboard data"
            message={tasksQuery.error instanceof Error ? tasksQuery.error.message : "Dashboard request failed"}
          />
        ) : null}
        {tasksQuery.isSuccess && tasks.length === 0 ? (
          <EmptyState title="No recent tasks" message="Run a generation job to populate dashboard activity." />
        ) : null}
        {tasks.length > 0 ? <TaskTable tasks={tasks.slice(0, 5)} /> : null}
      </section>
    </div>
  );
}
