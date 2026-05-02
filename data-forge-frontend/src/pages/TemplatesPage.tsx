import { FileStack, Plus } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { EmptyState } from "@/shared/components/EmptyState";
import { ErrorState } from "@/shared/components/ErrorState";
import { LoadingState } from "@/shared/components/LoadingState";
import { Button } from "@/shared/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";
import { TemplateForm } from "@/features/templates/TemplateForm";
import { TemplateTable } from "@/features/templates/TemplateTable";
import {
  useCreateTemplateMutation,
  useDeleteTemplateMutation,
  useGenerateFromTemplateMutation,
  useTemplatesQuery,
  useUpdateTemplateMutation,
} from "@/features/templates/templateQueries";
import { DataTemplate, GenerateRequest } from "@/shared/types/dataforge";

const defaultTemplateGenerateRequest: GenerateRequest = {
  count: 100,
  validate: true,
  output: {
    format: "JSON",
    encoding: "UTF-8",
  },
  fields: [{ name: "id", type: "uuid" }],
};

export function TemplatesPage() {
  const navigate = useNavigate();
  const [editingTemplate, setEditingTemplate] = useState<DataTemplate>();
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const templatesQuery = useTemplatesQuery();
  const createTemplateMutation = useCreateTemplateMutation();
  const updateTemplateMutation = useUpdateTemplateMutation();
  const deleteTemplateMutation = useDeleteTemplateMutation();
  const generateFromTemplateMutation = useGenerateFromTemplateMutation();

  const handleCreate = () => {
    setEditingTemplate(undefined);
    setIsFormOpen(true);
  };

  const handleSubmit = (template: DataTemplate) => {
    if (editingTemplate?.id) {
      updateTemplateMutation.mutate(
        { id: editingTemplate.id, template: { ...editingTemplate, ...template } },
        {
          onSuccess: () => {
            setIsFormOpen(false);
            setFeedback("Template updated successfully.");
          },
        },
      );
      return;
    }

    createTemplateMutation.mutate(template, {
      onSuccess: () => {
        setIsFormOpen(false);
        setFeedback("Template created successfully.");
      },
    });
  };

  const handleDelete = (template: DataTemplate) => {
    if (!template.id || !window.confirm(`Delete template "${template.name}"?`)) {
      return;
    }

    deleteTemplateMutation.mutate(template.id, {
      onSuccess: () => {
        setFeedback("Template deleted successfully.");
      },
    });
  };

  const handleGenerate = (template: DataTemplate) => {
    if (!template.id) {
      return;
    }

    const count = Number(window.prompt("Record count", "100"));
    const request = {
      ...defaultTemplateGenerateRequest,
      count: Number.isFinite(count) && count > 0 ? count : defaultTemplateGenerateRequest.count,
    };

    generateFromTemplateMutation.mutate(
      { templateId: template.id, request },
      {
        onSuccess: (taskId) => {
          navigate("/tasks", { state: { highlightedTaskId: taskId } });
        },
      },
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-6">
        <div>
          <p className="flex items-center gap-2 text-sm uppercase tracking-[0.28em] text-cyan-200">
            <FileStack className="size-4" aria-hidden="true" />
            Templates
          </p>
          <h1 className="mt-3 text-4xl font-semibold tracking-tight">Reuse generation configurations</h1>
        </div>
        <Button type="button" onClick={handleCreate}>
          <Plus className="size-4" aria-hidden="true" />
          New Template
        </Button>
      </div>

      {feedback ? (
        <p className="rounded-2xl border border-emerald-400/30 bg-emerald-950/20 px-4 py-3 text-sm text-emerald-100">
          {feedback}
        </p>
      ) : null}
      {templatesQuery.isPending ? <LoadingState label="Loading templates" /> : null}
      {templatesQuery.isError ? (
        <ErrorState
          title="Unable to load templates"
          message={templatesQuery.error instanceof Error ? templatesQuery.error.message : "Template request failed"}
        />
      ) : null}
      {templatesQuery.isSuccess && templatesQuery.data.length === 0 ? (
        <EmptyState title="No templates yet" message="Create a template to reuse a generation configuration." />
      ) : null}
      {templatesQuery.data && templatesQuery.data.length > 0 ? (
        <TemplateTable
          templates={templatesQuery.data}
          onEdit={(template) => {
            setEditingTemplate(template);
            setIsFormOpen(true);
          }}
          onDelete={handleDelete}
          onGenerate={handleGenerate}
        />
      ) : null}

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="border-slate-700/60 bg-slate-950 text-slate-50">
          <DialogHeader>
            <DialogTitle>{editingTemplate ? "Edit Template" : "Create Template"}</DialogTitle>
          </DialogHeader>
          <TemplateForm
            template={editingTemplate}
            isSubmitting={createTemplateMutation.isPending || updateTemplateMutation.isPending}
            onSubmit={handleSubmit}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
