import { apiRequest } from "@/shared/api/apiClient";
import { GenerateRequest, SyncGenerationResult } from "@/shared/types/dataforge";

export function generateSync(request: GenerateRequest) {
  return apiRequest<SyncGenerationResult>("/api/v1/dataforge/generate", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function generateAsync(request: GenerateRequest) {
  return apiRequest<number>("/api/v1/dataforge/generate/async", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
