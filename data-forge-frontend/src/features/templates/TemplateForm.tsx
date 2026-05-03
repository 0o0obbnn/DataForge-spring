import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Textarea } from "@/shared/components/ui/textarea";
import { DataTemplate } from "@/shared/types/dataforge";

const templateSchema = z.object({
  name: z.string().trim().min(1, "Template name is required"),
  description: z.string().optional(),
  config: z.string().trim().min(1, "Template config is required"),
  active: z.boolean().default(true),
});

type TemplateFormInput = z.input<typeof templateSchema>;
type TemplateFormValues = z.output<typeof templateSchema>;

interface TemplateFormProps {
  template?: DataTemplate;
  isSubmitting?: boolean;
  onSubmit: (template: DataTemplate) => void;
}

export function TemplateForm({ template, isSubmitting = false, onSubmit }: TemplateFormProps) {
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
      <label className="block space-y-2 text-sm">
        <span className="font-medium text-slate-300">Name</span>
        <Input className="border-slate-700/60 bg-slate-950/70" {...register("name")} />
        {errors.name ? <p className="text-sm text-rose-300">{errors.name.message}</p> : null}
      </label>

      <label className="block space-y-2 text-sm">
        <span className="font-medium text-slate-300">Description</span>
        <Input className="border-slate-700/60 bg-slate-950/70" {...register("description")} />
      </label>

      <label className="block space-y-2 text-sm">
        <span className="font-medium text-slate-300">Config YAML</span>
        <Textarea className="min-h-56 border-slate-700/60 bg-slate-950/70 font-mono text-xs" {...register("config")} />
        {errors.config ? <p className="text-sm text-rose-300">{errors.config.message}</p> : null}
      </label>

      <label className="flex items-center gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/40 px-4 py-3 text-sm text-slate-300">
        <input type="checkbox" className="size-4 accent-cyan-300" {...register("active")} />
        Active template
      </label>

      <Button className="w-full" type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saving..." : "Save Template"}
      </Button>
    </form>
  );
}
