import { ListChecks, RefreshCw } from "lucide-react";
import { useMemo, useState } from "react";

import { EmptyState } from "@/shared/components/EmptyState";
import { ErrorState } from "@/shared/components/ErrorState";
import { LoadingState } from "@/shared/components/LoadingState";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent } from "@/shared/components/ui/card";
import { Input } from "@/shared/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/components/ui/select";
import { TaskTable } from "@/features/tasks/TaskTable";
import { useRecentTasksQuery } from "@/features/tasks/taskQueries";

const statusFilters = ["ALL", "IN_PROGRESS", "COMPLETED", "FAILED"] as const;
type StatusFilter = (typeof statusFilters)[number];

export function TasksPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [taskQuery, setTaskQuery] = useState("");
  const tasksQuery = useRecentTasksQuery();
  const filteredTasks = useMemo(() => {
    const tasks = tasksQuery.data ?? [];
    const query = taskQuery.trim();

    return tasks.filter((task) => {
      const matchesStatus = statusFilter === "ALL" || task.status === statusFilter;
      const matchesQuery = query.length === 0 || String(task.id).includes(query);

      return matchesStatus && matchesQuery;
    });
  }, [statusFilter, taskQuery, tasksQuery.data]);

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-6">
        <div>
          <p className="flex items-center gap-2 text-sm uppercase tracking-[0.28em] text-cyan-200">
            <ListChecks className="size-4" aria-hidden="true" />
            Tasks
          </p>
          <h1 className="mt-3 text-4xl font-semibold tracking-tight">Monitor async generation</h1>
        </div>
        <Button type="button" variant="outline" onClick={() => void tasksQuery.refetch()} disabled={tasksQuery.isFetching}>
          <RefreshCw className="size-4" aria-hidden="true" />
          {tasksQuery.isFetching ? "Refreshing..." : "Refresh"}
        </Button>
      </div>

      <Card className="border-slate-700/50 bg-slate-950/60 text-slate-50">
        <CardContent className="grid gap-4 p-4 md:grid-cols-[1fr_14rem]">
          <Input
            value={taskQuery}
            onChange={(event) => setTaskQuery(event.target.value)}
            placeholder="Search by task id..."
            className="border-slate-700/60 bg-slate-950/70"
            aria-label="Search tasks by id"
          />
          <Select value={statusFilter} onValueChange={(value) => setStatusFilter(value as StatusFilter)}>
            <SelectTrigger className="w-full border-slate-700/60 bg-slate-950/70">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {statusFilters.map((status) => (
                <SelectItem key={status} value={status}>
                  {status === "ALL" ? "All statuses" : status.replace("_", " ")}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {tasksQuery.isPending ? <LoadingState label="Loading recent generation tasks" /> : null}
      {tasksQuery.isError ? (
        <ErrorState
          title="Unable to load tasks"
          message={tasksQuery.error instanceof Error ? tasksQuery.error.message : "Task request failed"}
        />
      ) : null}
      {tasksQuery.isSuccess && filteredTasks.length === 0 ? (
        <EmptyState title="No tasks found" message="No recent generation tasks match the current filters." />
      ) : null}
      {filteredTasks.length > 0 ? <TaskTable tasks={filteredTasks} /> : null}
    </div>
  );
}
