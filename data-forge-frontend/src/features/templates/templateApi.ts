import { apiRequest } from "@/shared/api/apiClient";
import { DataTemplate, GenerateRequest } from "@/shared/types/dataforge";

export function listTemplates() {
  return apiRequest<DataTemplate[]>("/api/v1/templates");
}

export function createTemplate(template: DataTemplate) {
  return apiRequest<DataTemplate>("/api/v1/templates", {
    method: "POST",
    body: JSON.stringify(template),
  });
}

export function updateTemplate(id: number, template: DataTemplate) {
  return apiRequest<DataTemplate>(`/api/v1/templates/${id}`, {
    method: "PUT",
    body: JSON.stringify(template),
  });
}

export function deleteTemplate(id: number) {
  return apiRequest<void>(`/api/v1/templates/${id}`, {
    method: "DELETE",
  });
}

export function generateFromTemplate(templateId: number, request: GenerateRequest) {
  return apiRequest<number>(`/api/v1/dataforge/generate/template/${templateId}`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}
