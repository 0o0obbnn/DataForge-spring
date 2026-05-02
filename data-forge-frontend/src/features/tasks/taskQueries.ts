import { useQuery } from "@tanstack/react-query";

import { listRecentTasks } from "@/features/tasks/taskApi";
import { GenerationTask } from "@/shared/types/dataforge";

export const taskQueryKeys = {
  recent: (page: number, size: number) => ["tasks", "recent", page, size] as const,
};

export function shouldPollTasks(tasks: GenerationTask[] | undefined) {
  return tasks?.some((task) => task.status === "IN_PROGRESS") ?? false;
}

export function useRecentTasksQuery(page = 0, size = 10) {
  return useQuery({
    queryKey: taskQueryKeys.recent(page, size),
    queryFn: () => listRecentTasks(page, size),
    refetchInterval: (query) => (shouldPollTasks(query.state.data) ? 2_000 : false),
  });
}
