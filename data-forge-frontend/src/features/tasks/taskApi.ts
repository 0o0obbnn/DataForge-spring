import { apiRequest } from "@/shared/api/apiClient";
import { GenerationTask } from "@/shared/types/dataforge";

export function listRecentTasks(page = 0, size = 10) {
  return apiRequest<GenerationTask[]>(`/api/v1/dataforge/tasks?page=${page}&size=${size}`);
}

export function getTask(taskId: number) {
  return apiRequest<GenerationTask>(`/api/v1/dataforge/tasks/${taskId}`);
}
