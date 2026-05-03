import { ArrowRight, Boxes, DatabaseZap, ListChecks } from "lucide-react";
import { useTranslation } from "react-i18next";
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

export function DashboardPage() {
  const { t } = useTranslation(["pages", "common"]);
  const tasksQuery = useDashboardTasksQuery();
  const tasks = tasksQuery.data ?? [];
  const stats = deriveDashboardStats(tasks);

  const quickActions = [
    { label: t("common:actions.newGeneration"), href: "/builder", icon: DatabaseZap },
    { label: t("common:actions.browseCatalog"), href: "/catalog", icon: Boxes },
    { label: t("common:actions.monitorTasks"), href: "/tasks", icon: ListChecks },
  ];

  return (
    <div className="space-y-6">
      <section className="flex items-start justify-between gap-6">
        <div>
          <StatusBadge status="healthy">{t("common:status.apiReady")}</StatusBadge>
          <h1 className="mt-3 text-2xl font-semibold text-slate-100">{t("pages:dashboard.title")}</h1>
          <p className="mt-1 max-w-2xl text-sm text-slate-400">{t("pages:dashboard.subtitle")}</p>
        </div>
      </section>

      <DashboardStats stats={stats} />

      <section className="grid gap-3 md:grid-cols-3">
        {quickActions.map((action) => (
          <Card key={action.href} className="border-slate-800 bg-slate-900">
            <CardContent className="flex items-center justify-between gap-4 p-4">
              <div className="flex items-center gap-3">
                <action.icon className="size-5 text-blue-400" aria-hidden="true" />
                <span className="font-medium text-slate-200">{action.label}</span>
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

      <section className="space-y-3">
        <div>
          <h2 className="text-lg font-medium text-slate-100">{t("pages:dashboard.recentTasks")}</h2>
          <p className="mt-0.5 text-sm text-slate-500">{t("pages:dashboard.recentTasksDesc")}</p>
        </div>
        {tasksQuery.isPending ? <LoadingState label={t("common:status.loadingDashboard")} /> : null}
        {tasksQuery.isError ? (
          <ErrorState
            title={t("common:error.loadDashboard")}
            message={tasksQuery.error instanceof Error ? tasksQuery.error.message : t("common:error.unknown")}
          />
        ) : null}
        {tasksQuery.isSuccess && tasks.length === 0 ? (
          <EmptyState title={t("common:empty.noTasks")} message={t("common:empty.noTasksMessage")} />
        ) : null}
        {tasks.length > 0 ? <TaskTable tasks={tasks.slice(0, 5)} /> : null}
      </section>
    </div>
  );
}
