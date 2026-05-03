import { Search } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { TaskTable } from "@/features/tasks/TaskTable";
import { useRecentTasksQuery } from "@/features/tasks/taskQueries";
import { EmptyState } from "@/shared/components/EmptyState";
import { ErrorState } from "@/shared/components/ErrorState";
import { LoadingState } from "@/shared/components/LoadingState";
import { Input } from "@/shared/components/ui/input";

export function TasksPage() {
  const { t } = useTranslation(["pages", "common"]);
  const [search, setSearch] = useState("");
  const tasksQuery = useRecentTasksQuery();
  const tasks = (tasksQuery.data ?? []).filter((task) =>
    search ? String(task.id).includes(search) : true,
  );

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold text-slate-100">{t("pages:tasks.title")}</h1>
        <p className="mt-1 text-sm text-slate-400">{t("pages:tasks.subtitle")}</p>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-500" aria-hidden="true" />
        <Input
          placeholder={t("common:form.searchTasks")}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="border-slate-700 bg-slate-900 pl-9 text-slate-100"
          aria-label={t("common:form.searchTasks")}
        />
      </div>

      {tasksQuery.isPending ? <LoadingState label={t("common:status.loadingTasks")} /> : null}
      {tasksQuery.isError ? (
        <ErrorState
          title={t("common:error.loadTasks")}
          message={tasksQuery.error instanceof Error ? tasksQuery.error.message : t("common:error.unknown")}
        />
      ) : null}
      {tasksQuery.isSuccess && tasks.length === 0 ? (
        <EmptyState title={t("common:empty.noTaskFilters")} message={t("common:empty.noTaskFiltersMessage")} />
      ) : null}
      {tasks.length > 0 ? <TaskTable tasks={tasks} /> : null}
    </div>
  );
}
