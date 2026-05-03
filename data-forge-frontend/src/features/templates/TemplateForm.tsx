import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { z } from "zod";

import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Textarea } from "@/shared/components/ui/textarea";
import { DataTemplate } from "@/shared/types/dataforge";

interface TemplateFormProps {
  template?: DataTemplate;
  isSubmitting?: boolean;
  onSubmit: (template: DataTemplate) => void;
}

export function TemplateForm({ template, isSubmitting = false, onSubmit }: TemplateFormProps) {
  const { t } = useTranslation("common");

  const templateSchema = z.object({
    name: z.string().trim().min(1, t("error.nameRequired")),
    description: z.string().optional(),
    config: z.string().trim().min(1, t("error.configRequired")),
    active: z.boolean().default(true),
  });

  type TemplateFormInput = z.input<typeof templateSchema>;
  type TemplateFormValues = z.output<typeof templateSchema>;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TemplateFormInput, unknown, TemplateFormValues>({
    resolver: zodResolver(templateSchema),
    defaultValues: {
      name: "",
      description: "",
      config: "",
      active: true,
    },
  });

  useEffect(() => {
    reset({
      name: template?.name ?? "",
      description: template?.description ?? "",
      config: template?.config ?? "",
      active: template?.active ?? true,
    });
  }, [reset, template]);

  return (
    <form className="space-y-4" onSubmit={handleSubmit((values) => onSubmit(values))} noValidate>
      <label className="block space-y-1.5 text-sm">
        <span className="font-medium text-slate-300">{t("form.templateName")}</span>
        <Input className="border-slate-700 bg-slate-950" {...register("name")} />
        {errors.name ? <p className="text-sm text-red-400">{errors.name.message}</p> : null}
      </label>

      <label className="block space-y-1.5 text-sm">
        <span className="font-medium text-slate-300">{t("form.description")}</span>
        <Input className="border-slate-700 bg-slate-950" {...register("description")} />
      </label>

      <label className="block space-y-1.5 text-sm">
        <span className="font-medium text-slate-300">{t("form.configYaml")}</span>
        <Textarea className="min-h-52 border-slate-700 bg-slate-950 font-mono text-xs" {...register("config")} />
        {errors.config ? <p className="text-sm text-red-400">{errors.config.message}</p> : null}
      </label>

      <label className="flex items-center gap-3 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-sm text-slate-300">
        <input type="checkbox" className="size-4 accent-blue-600" {...register("active")} />
        {t("form.activeTemplate")}
      </label>

      <Button className="w-full" type="submit" disabled={isSubmitting}>
        {isSubmitting ? t("status.saving") : t("actions.save")}
      </Button>
    </form>
  );
}
