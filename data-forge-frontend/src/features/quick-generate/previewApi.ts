import { apiRequest } from "@/shared/api/apiClient";

export interface GeneratePreviewRequest {
  generatorType: string;
  count: number;
  params?: Record<string, unknown>;
}

export type PreviewRecord = Record<string, unknown>;

export function generatePreview(request: GeneratePreviewRequest) {
  return apiRequest<PreviewRecord[]>("/api/v1/dataforge/generate/preview", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
