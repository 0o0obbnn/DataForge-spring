import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  createTemplate,
  deleteTemplate,
  generateFromTemplate,
  listTemplates,
  updateTemplate,
} from "@/features/templates/templateApi";
import { DataTemplate, GenerateRequest } from "@/shared/types/dataforge";

export const templateQueryKeys = {
  all: ["templates"] as const,
};

export function useTemplatesQuery() {
  return useQuery({
    queryKey: templateQueryKeys.all,
    queryFn: listTemplates,
  });
}

export function useCreateTemplateMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createTemplate,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: templateQueryKeys.all });
    },
  });
}

export function useUpdateTemplateMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, template }: { id: number; template: DataTemplate }) => updateTemplate(id, template),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: templateQueryKeys.all });
    },
  });
}

export function useDeleteTemplateMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteTemplate,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: templateQueryKeys.all });
    },
  });
}

export function useGenerateFromTemplateMutation() {
  return useMutation({
    mutationFn: ({ templateId, request }: { templateId: number; request: GenerateRequest }) =>
      generateFromTemplate(templateId, request),
  });
}
