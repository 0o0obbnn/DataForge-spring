import { useState } from "react";
import { useTranslation } from "react-i18next";

import { TemplateForm } from "@/features/templates/TemplateForm";
import { TemplateTable } from "@/features/templates/TemplateTable";
import {
  useCreateTemplateMutation,
  useDeleteTemplateMutation,
  useTemplatesQuery,
  useUpdateTemplateMutation,
} from "@/features/templates/templateQueries";
import { DataTemplate } from "@/shared/types/dataforge";
import { EmptyState } from "@/shared/components/EmptyState";
import { ErrorState } from "@/shared/components/ErrorState";
import { LoadingState } from "@/shared/components/LoadingState";
import { Button } from "@/shared/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/components/ui/dialog";

export function TemplatesPage() {
  const { t } = useTranslation(["pages", "common"]);
  const [editingTemplate, setEditingTemplate] = useState<DataTemplate | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const templatesQuery = useTemplatesQuery();
  const createMutation = useCreateTemplateMutation();
  const updateMutation = useUpdateTemplateMutation();
  const deleteMutation = useDeleteTemplateMutation();

  const templates = templatesQuery.data ?? [];

  const handleCreate = async (values: { name: string; description?: string; config: string }) => {
    await createMutation.mutateAsync(values);
    setIsCreateOpen(false);
  };

  const handleUpdate = async (values: { name: string; description?: string; config: string }) => {
    if (!editingTemplate?.id) return;
    await updateMutation.mutateAsync({ id: editingTemplate.id, template: values as DataTemplate });
    setEditingTemplate(null);
  };

  const handleDelete = async (template: DataTemplate) => {
    if (!template.id) return;
    if (window.confirm(`${t("common:actions.delete")} "${template.name}"?`)) {
      await deleteMutation.mutateAsync(template.id);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-100">{t("pages:templates.title")}</h1>
          <p className="mt-1 text-sm text-slate-400">{t("pages:templates.subtitle")}</p>
        </div>
        <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
          <DialogTrigger asChild>
            <Button>{t("common:actions.createTemplate")}</Button>
          </DialogTrigger>
          <DialogContent className="border-slate-700 bg-slate-900 text-slate-100">
            <DialogHeader>
              <DialogTitle>{t("common:actions.createTemplate")}</DialogTitle>
            </DialogHeader>
            <TemplateForm onSubmit={handleCreate} />
          </DialogContent>
        </Dialog>
      </div>

      {templatesQuery.isPending ? <LoadingState label={t("common:status.loadingTemplates")} /> : null}
      {templatesQuery.isError ? (
        <ErrorState
          title={t("common:error.loadTemplates")}
          message={templatesQuery.error instanceof Error ? templatesQuery.error.message : t("common:error.unknown")}
        />
      ) : null}
      {templatesQuery.isSuccess && templates.length === 0 ? (
        <EmptyState title={t("common:empty.noTemplates")} message={t("common:empty.noTemplatesMessage")} />
      ) : null}
      {templates.length > 0 ? (
        <TemplateTable templates={templates} onEdit={setEditingTemplate} onDelete={handleDelete} onGenerate={() => {}} />
      ) : null}

      {editingTemplate ? (
        <Dialog open onOpenChange={() => setEditingTemplate(null)}>
          <DialogContent className="border-slate-700 bg-slate-900 text-slate-100">
            <DialogHeader>
              <DialogTitle>{t("common:actions.edit")}</DialogTitle>
            </DialogHeader>
            <TemplateForm
              template={editingTemplate}
              onSubmit={handleUpdate}
            />
          </DialogContent>
        </Dialog>
      ) : null}
    </div>
  );
}
