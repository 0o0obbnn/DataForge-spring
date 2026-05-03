import { Pencil, Play, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";

import { StatusBadge } from "@/shared/components/StatusBadge";
import { Button } from "@/shared/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/components/ui/table";
import { DataTemplate } from "@/shared/types/dataforge";

interface TemplateTableProps {
  templates: DataTemplate[];
  onEdit: (template: DataTemplate) => void;
  onDelete: (template: DataTemplate) => void;
  onGenerate: (template: DataTemplate) => void;
}

function formatDateTime(value?: string) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function TemplateTable({ templates, onEdit, onDelete, onGenerate }: TemplateTableProps) {
  const { t } = useTranslation("common");

  return (
    <div className="overflow-hidden rounded-lg border border-slate-800 bg-slate-900">
      <Table>
        <TableHeader>
          <TableRow className="border-slate-800 hover:bg-transparent">
            <TableHead className="text-slate-400">{t("form.fieldName")}</TableHead>
            <TableHead className="text-slate-400">{t("status.status")}</TableHead>
            <TableHead className="text-slate-400">{t("status.version")}</TableHead>
            <TableHead className="text-slate-400">{t("status.updated")}</TableHead>
            <TableHead className="text-right text-slate-400">{t("actions.actions")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {templates.map((template) => (
            <TableRow key={template.id ?? template.name} className="border-slate-800 hover:bg-slate-800">
              <TableCell>
                <div>
                  <p className="font-medium text-slate-100">{template.name}</p>
                  <p className="mt-1 max-w-xl truncate text-sm text-slate-500">{template.description ?? t("empty.noDescription")}</p>
                </div>
              </TableCell>
              <TableCell>
                <StatusBadge status={template.active ?? true ? "completed" : "warning"}>
                  {template.active ?? true ? t("status.active") : t("status.inactive")}
                </StatusBadge>
              </TableCell>
              <TableCell className="font-mono text-slate-400">{template.version ?? "-"}</TableCell>
              <TableCell className="text-slate-500">{formatDateTime(template.updatedAt)}</TableCell>
              <TableCell>
                <div className="flex justify-end gap-2">
                  <Button type="button" size="icon-sm" variant="outline" onClick={() => onGenerate(template)} aria-label={t("actions.generate")}>
                    <Play className="size-4" aria-hidden="true" />
                  </Button>
                  <Button type="button" size="icon-sm" variant="outline" onClick={() => onEdit(template)} aria-label={t("actions.edit")}>
                    <Pencil className="size-4" aria-hidden="true" />
                  </Button>
                  <Button type="button" size="icon-sm" variant="outline" onClick={() => onDelete(template)} aria-label={t("actions.delete")}>
                    <Trash2 className="size-4" aria-hidden="true" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
