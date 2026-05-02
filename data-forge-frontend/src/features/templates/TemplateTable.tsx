import { Play, Pencil, Trash2 } from "lucide-react";

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

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function TemplateTable({ templates, onEdit, onDelete, onGenerate }: TemplateTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-700/50 bg-slate-950/60">
      <Table>
        <TableHeader>
          <TableRow className="border-slate-800 hover:bg-transparent">
            <TableHead className="text-slate-300">Name</TableHead>
            <TableHead className="text-slate-300">Status</TableHead>
            <TableHead className="text-slate-300">Version</TableHead>
            <TableHead className="text-slate-300">Updated</TableHead>
            <TableHead className="text-right text-slate-300">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {templates.map((template) => (
            <TableRow key={template.id ?? template.name} className="border-slate-800/80 hover:bg-slate-900/70">
              <TableCell>
                <div>
                  <p className="font-medium text-slate-100">{template.name}</p>
                  <p className="mt-1 max-w-xl truncate text-sm text-slate-500">{template.description ?? "No description"}</p>
                </div>
              </TableCell>
              <TableCell>
                <StatusBadge status={template.active ?? true ? "completed" : "warning"}>
                  {template.active ?? true ? "Active" : "Inactive"}
                </StatusBadge>
              </TableCell>
              <TableCell className="font-mono text-slate-300">{template.version ?? "-"}</TableCell>
              <TableCell className="text-slate-400">{formatDateTime(template.updatedAt)}</TableCell>
              <TableCell>
                <div className="flex justify-end gap-2">
                  <Button type="button" size="icon-sm" variant="outline" onClick={() => onGenerate(template)} aria-label="Generate from template">
                    <Play className="size-4" aria-hidden="true" />
                  </Button>
                  <Button type="button" size="icon-sm" variant="outline" onClick={() => onEdit(template)} aria-label="Edit template">
                    <Pencil className="size-4" aria-hidden="true" />
                  </Button>
                  <Button type="button" size="icon-sm" variant="outline" onClick={() => onDelete(template)} aria-label="Delete template">
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
