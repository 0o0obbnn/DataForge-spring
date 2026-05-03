import { useRecentTasksQuery } from "@/features/tasks/taskQueries";
import { GenerationTask } from "@/shared/types/dataforge";

export interface DashboardStats {
  total: number;
  completed: number;
  failed: number;
  running: number;
  averageDurationMs: number;
  throughputRecords: number;
}

export function deriveDashboardStats(tasks: GenerationTask[] = []): DashboardStats {
  const completedTasks = tasks.filter((task) => task.status === "COMPLETED");
  const completedDurations = completedTasks
    .map((task) => task.durationMs)
    .filter((duration): duration is number => duration !== undefined);
  const durationTotal = completedDurations.reduce((total, duration) => total + duration, 0);

  return {
    total: tasks.length,
    completed: completedTasks.length,
    failed: tasks.filter((task) => task.status === "FAILED").length,
    running: tasks.filter((task) => task.status === "IN_PROGRESS").length,
    averageDurationMs: completedDurations.length > 0 ? durationTotal / completedDurations.length : 0,
    throughputRecords: completedTasks.reduce((total, task) => total + task.recordCount, 0),
  };
}

export function useDashboardTasksQuery() {
  return useRecentTasksQuery(0, 10);
}
