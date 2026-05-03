import { useTranslation } from "react-i18next";

import { StatusBadge } from "@/shared/components/StatusBadge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/components/ui/table";
import { formatDateTime, formatDuration, formatNumber } from "@/shared/lib/format";
import { GenerationTask, GenerationTaskStatus } from "@/shared/types/dataforge";

interface TaskTableProps {
  tasks: GenerationTask[];
}

const statusMap: Record<GenerationTaskStatus, "running" | "completed" | "failed"> = {
  IN_PROGRESS: "running",
  COMPLETED: "completed",
  FAILED: "failed",
};

export function TaskTable({ tasks }: TaskTableProps) {
  const { t } = useTranslation("common");

  return (
    <div className="overflow-hidden rounded-lg border border-slate-800 bg-slate-900">
      <Table>
        <TableHeader>
          <TableRow className="border-slate-800 hover:bg-transparent">
            <TableHead className="text-slate-400">ID</TableHead>
            <TableHead className="text-slate-400">{t("status.status")}</TableHead>
            <TableHead className="text-slate-400">{t("form.recordCount")}</TableHead>
            <TableHead className="text-slate-400">{t("status.duration")}</TableHead>
            <TableHead className="text-slate-400">{t("status.created")}</TableHead>
            <TableHead className="text-slate-400">{t("status.completed")}</TableHead>
            <TableHead className="text-slate-400">{t("status.error")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tasks.map((task) => (
            <TableRow key={task.id} className="border-slate-800 hover:bg-slate-800">
              <TableCell className="font-mono text-blue-300">#{task.id}</TableCell>
              <TableCell>
                <StatusBadge status={statusMap[task.status]}>{task.status.replace("_", " ")}</StatusBadge>
              </TableCell>
              <TableCell className="text-slate-200">{formatNumber(task.recordCount)}</TableCell>
              <TableCell className="text-slate-400">{formatDuration(task.durationMs)}</TableCell>
              <TableCell className="text-slate-400">{formatDateTime(task.createdAt)}</TableCell>
              <TableCell className="text-slate-400">{formatDateTime(task.completedAt)}</TableCell>
              <TableCell className="max-w-xs truncate text-slate-500">{task.errorMessage ?? "-"}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
