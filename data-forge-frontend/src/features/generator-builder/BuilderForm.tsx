import { useMutation } from "@tanstack/react-query";
import { Copy, Plus, RotateCcw, Trash2 } from "lucide-react";
import { FormEvent, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { RequestPreview } from "@/features/generator-builder/RequestPreview";
import { generateRequestSchema } from "@/features/generator-builder/builderSchema";
import { useBuilderStore } from "@/features/generator-builder/builderStore";
import { generateAsync, generateSync } from "@/features/generator-builder/generationApi";
import { generatorCatalog } from "@/features/generator-catalog/generatorCatalogData";
import { createTemplate } from "@/features/templates/templateApi";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";
import { Input } from "@/shared/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/components/ui/select";
import { Textarea } from "@/shared/components/ui/textarea";

const outputFormats = ["CSV", "JSON", "SQL", "CONSOLE"] as const;

function parseOptionalNumber(value: string) {
  return value.trim().length > 0 ? Number(value) : undefined;
}

function parseParams(value: string) {
  if (value.trim().length === 0) {
    return {};
  }

  const parsed = JSON.parse(value) as unknown;
  return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? (parsed as Record<string, unknown>) : {};
}

export function BuilderForm() {
  const navigate = useNavigate();
  const { t } = useTranslation("common");
  const draft = useBuilderStore((state) => state.draft);
  const addField = useBuilderStore((state) => state.addField);
  const duplicateField = useBuilderStore((state) => state.duplicateField);
  const removeField = useBuilderStore((state) => state.removeField);
  const resetDraft = useBuilderStore((state) => state.resetDraft);
  const updateDraft = useBuilderStore((state) => state.updateDraft);
  const updateField = useBuilderStore((state) => state.updateField);
  const updateOutput = useBuilderStore((state) => state.updateOutput);
  const [validationError, setValidationError] = useState<string>();
  const [syncMessage, setSyncMessage] = useState<string>();
  const [isSaveTemplateDialogOpen, setIsSaveTemplateDialogOpen] = useState(false);
  const [templateName, setTemplateName] = useState("");
  const [templateDescription, setTemplateDescription] = useState("");
  const syncMutation = useMutation({ mutationFn: generateSync });
  const asyncMutation = useMutation({ mutationFn: generateAsync });
  const saveTemplateMutation = useMutation({ mutationFn: createTemplate });

  const handlePendingSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
  };

  const parseDraft = () => {
    const result = generateRequestSchema.safeParse(draft);

    if (!result.success) {
      setSyncMessage(undefined);
      setValidationError(result.error.issues[0]?.message ?? t("builder.invalidRequest"));
      return undefined;
    }

    setValidationError(undefined);
    return result.data;
  };

  const handleRunSync = async () => {
    const request = parseDraft();

    if (!request) {
      return;
    }

    try {
      const result = await syncMutation.mutateAsync(request);
      setSyncMessage(result.outputPath ? `${result.message} Output: ${result.outputPath}` : result.message);
    } catch (error) {
      setSyncMessage(undefined);
      setValidationError(error instanceof Error ? error.message : t("builder.syncFailed"));
    }
  };

  const handleRunAsync = async () => {
    const request = parseDraft();

    if (!request) {
      return;
    }

    try {
      const taskId = await asyncMutation.mutateAsync(request);
      navigate("/tasks", { state: { highlightedTaskId: taskId } });
    } catch (error) {
      setValidationError(error instanceof Error ? error.message : t("builder.asyncFailed"));
    }
  };

  const handleOpenSaveTemplateDialog = () => {
    const request = parseDraft();
    if (!request) {
      return;
    }

    const suggestedName = `template-${new Date().toISOString().replace(/[:.]/g, "-")}`;
    setTemplateName(suggestedName);
    setTemplateDescription(t("builder.savedFromBuilder"));
    setIsSaveTemplateDialogOpen(true);
  };

  const handleSaveTemplate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const request = parseDraft();
    if (!request) {
      return;
    }

    const normalizedName = templateName.trim();
    if (normalizedName.length === 0) {
      setValidationError(t("error.nameRequired"));
      return;
    }

    try {
      await saveTemplateMutation.mutateAsync({
        name: normalizedName,
        description: templateDescription.trim() || undefined,
        config: JSON.stringify(request, null, 2),
        active: true,
      });
      setIsSaveTemplateDialogOpen(false);
      setValidationError(undefined);
      setSyncMessage(`Template "${normalizedName}" saved successfully.`);
    } catch (error) {
      setValidationError(error instanceof Error ? error.message : t("builder.saveFailed"));
    }
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_28rem]">
      <form className="space-y-6" onSubmit={handlePendingSubmit}>
        <Card className="border-slate-800 bg-slate-900 text-slate-100">
          <CardHeader>
            <CardTitle>Execution Controls</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            <label className="space-y-2 text-sm">
              <span className="font-medium text-slate-300">Count</span>
              <Input
                type="number"
                min={1}
                max={1_000_000}
                value={draft.count}
                onChange={(event) => updateDraft({ count: Number(event.target.value) })}
                className="border-slate-700 bg-slate-950"
              />
            </label>
            <label className="space-y-2 text-sm">
              <span className="font-medium text-slate-300">Threads</span>
              <Input
                type="number"
                min={1}
                max={16}
                value={draft.threads ?? ""}
                onChange={(event) => updateDraft({ threads: parseOptionalNumber(event.target.value) })}
                className="border-slate-700 bg-slate-950"
              />
            </label>
            <label className="space-y-2 text-sm">
              <span className="font-medium text-slate-300">Seed</span>
              <Input
                type="number"
                value={draft.seed ?? ""}
                onChange={(event) => updateDraft({ seed: parseOptionalNumber(event.target.value) })}
                className="border-slate-700 bg-slate-950"
              />
            </label>
            <label className="space-y-2 text-sm">
              <span className="font-medium text-slate-300">Output Format</span>
              <Select value={draft.output.format} onValueChange={(format) => updateOutput({ format: format as typeof draft.output.format })}>
                <SelectTrigger className="w-full border-slate-700 bg-slate-950">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {outputFormats.map((format) => (
                    <SelectItem key={format} value={format}>
                      {format}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </label>
            <label className="space-y-2 text-sm md:col-span-2">
              <span className="font-medium text-slate-300">Output File</span>
              <Input
                value={draft.output.file ?? ""}
                placeholder={t("form.outputPath")}
                onChange={(event) => updateOutput({ file: event.target.value || undefined })}
                className="border-slate-700 bg-slate-950"
              />
            </label>
            <label className="flex items-center gap-3 rounded-lg border border-slate-700/60 bg-slate-800 px-4 py-3 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={draft.validate}
                onChange={(event) => updateDraft({ validate: event.target.checked })}
                className="size-4 accent-blue-600"
              />
              Validate generated records
            </label>
          </CardContent>
        </Card>

        <Card className="border-slate-800 bg-slate-900 text-slate-100">
          <CardHeader className="flex-row items-center justify-between">
            <CardTitle>Fields</CardTitle>
            <div className="flex gap-2">
              <Select onValueChange={addField}>
                <SelectTrigger className="w-44 border-slate-700 bg-slate-950">
                  <SelectValue placeholder={t("form.addGenerator")} />
                </SelectTrigger>
                <SelectContent>
                  {generatorCatalog.map((generator) => (
                    <SelectItem key={generator.id} value={generator.id}>
                      {generator.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button type="button" variant="outline" onClick={() => addField("string")}>
                <Plus className="size-4" aria-hidden="true" />
                Add Field
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {draft.fields.length > 0 ? (
              draft.fields.map((field, index) => (
                <div key={`${field.type}-${index}`} className="rounded-lg border border-slate-800 bg-slate-800 p-4">
                  <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
                    <label className="space-y-2 text-sm">
                      <span className="font-medium text-slate-300">Field Name</span>
                      <Input
                        value={field.name}
                        onChange={(event) => updateField(index, { name: event.target.value })}
                        className="border-slate-700 bg-slate-950"
                      />
                    </label>
                    <label className="space-y-2 text-sm">
                      <span className="font-medium text-slate-300">Generator Type</span>
                      <Select value={field.type} onValueChange={(type) => updateField(index, { type })}>
                        <SelectTrigger className="w-full border-slate-700 bg-slate-950">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {generatorCatalog.map((generator) => (
                            <SelectItem key={generator.id} value={generator.id}>
                              {generator.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </label>
                    <div className="flex items-end gap-2">
                      <Button type="button" variant="outline" size="icon" onClick={() => duplicateField(index)} aria-label={t("actions.duplicate")}>
                        <Copy className="size-4" aria-hidden="true" />
                      </Button>
                      <Button type="button" variant="outline" size="icon" onClick={() => removeField(index)} aria-label={t("actions.delete")}>
                        <Trash2 className="size-4" aria-hidden="true" />
                      </Button>
                    </div>
                  </div>
                  <label className="mt-3 block space-y-2 text-sm">
                    <span className="font-medium text-slate-300">Params JSON</span>
                    <Textarea
                      defaultValue={JSON.stringify(field.params ?? {}, null, 2)}
                      onBlur={(event) => updateField(index, { params: parseParams(event.target.value) })}
                      className="min-h-20 border-slate-700 bg-slate-950 font-mono text-xs"
                    />
                  </label>
                </div>
              ))
            ) : (
              <div className="rounded-lg border border-dashed border-slate-700 bg-slate-800/50 p-8 text-center text-sm text-slate-400">
                Add a generator field from the catalog or the selector above.
              </div>
            )}
          </CardContent>
        </Card>

        <div className="flex flex-wrap gap-3">
          <Button type="button" onClick={handleRunSync} disabled={syncMutation.isPending || asyncMutation.isPending}>
            {syncMutation.isPending ? t("builder.runningSync") : t("builder.runSync")}
          </Button>
          <Button type="button" variant="outline" onClick={handleRunAsync} disabled={syncMutation.isPending || asyncMutation.isPending}>
            {asyncMutation.isPending ? t("builder.submitting") : t("builder.runAsync")}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={handleOpenSaveTemplateDialog}
            disabled={syncMutation.isPending || asyncMutation.isPending || saveTemplateMutation.isPending}
          >
            Save as Template
          </Button>
          <Button type="button" variant="ghost" onClick={resetDraft}>
            <RotateCcw className="size-4" aria-hidden="true" />
            Reset
          </Button>
        </div>
        {validationError ? (
          <p className="rounded-lg border border-red-800 bg-red-950/30 px-4 py-3 text-sm text-red-300">
            {validationError}
          </p>
        ) : null}
        {syncMessage ? (
          <p className="rounded-lg border border-emerald-800 bg-emerald-950/30 px-4 py-3 text-sm text-emerald-300">
            {syncMessage}
          </p>
        ) : null}
      </form>

      <RequestPreview draft={draft} />

      <Dialog open={isSaveTemplateDialogOpen} onOpenChange={setIsSaveTemplateDialogOpen}>
        <DialogContent className="border-slate-700/60 bg-slate-950 text-slate-50">
          <DialogHeader>
            <DialogTitle>Save as Template</DialogTitle>
          </DialogHeader>
          <form className="space-y-4" onSubmit={handleSaveTemplate}>
            <label className="block space-y-2 text-sm">
              <span className="font-medium text-slate-300">Template Name</span>
              <Input
                value={templateName}
                onChange={(event) => setTemplateName(event.target.value)}
                className="border-slate-700 bg-slate-950"
              />
            </label>
            <label className="block space-y-2 text-sm">
              <span className="font-medium text-slate-300">Description</span>
              <Input
                value={templateDescription}
                onChange={(event) => setTemplateDescription(event.target.value)}
                className="border-slate-700 bg-slate-950"
              />
            </label>
            <Button className="w-full" type="submit" disabled={saveTemplateMutation.isPending}>
              {saveTemplateMutation.isPending ? t("status.saving") : t("actions.save")}
            </Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
