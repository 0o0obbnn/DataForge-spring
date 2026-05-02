import { GenerationTask, GenerationTaskStatus } from "@/shared/types/dataforge";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/components/ui/table";
import { formatDateTime, formatDuration, formatNumber } from "@/shared/lib/format";

interface TaskTableProps {
  tasks: GenerationTask[];
}

const statusMap: Record<GenerationTaskStatus, "running" | "completed" | "failed"> = {
  IN_PROGRESS: "running",
  COMPLETED: "completed",
  FAILED: "failed",
};

export function TaskTable({ tasks }: TaskTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-700/50 bg-slate-950/60">
      <Table>
        <TableHeader>
          <TableRow className="border-slate-800 hover:bg-transparent">
            <TableHead className="text-slate-300">ID</TableHead>
            <TableHead className="text-slate-300">Status</TableHead>
            <TableHead className="text-slate-300">Records</TableHead>
            <TableHead className="text-slate-300">Duration</TableHead>
            <TableHead className="text-slate-300">Created</TableHead>
            <TableHead className="text-slate-300">Completed</TableHead>
            <TableHead className="text-slate-300">Failure Reason</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tasks.map((task) => (
            <TableRow key={task.id} className="border-slate-800/80 hover:bg-slate-900/70">
              <TableCell className="font-mono text-cyan-100">#{task.id}</TableCell>
              <TableCell>
                <StatusBadge status={statusMap[task.status]}>{task.status.replace("_", " ")}</StatusBadge>
              </TableCell>
              <TableCell className="text-slate-200">{formatNumber(task.recordCount)}</TableCell>
              <TableCell className="text-slate-300">{formatDuration(task.durationMs)}</TableCell>
              <TableCell className="text-slate-400">{formatDateTime(task.createdAt)}</TableCell>
              <TableCell className="text-slate-400">{formatDateTime(task.completedAt)}</TableCell>
              <TableCell className="max-w-xs truncate text-slate-400">{task.errorMessage ?? "-"}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
