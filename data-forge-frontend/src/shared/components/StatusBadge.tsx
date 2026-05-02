import { Badge } from "@/shared/components/ui/badge";
import { cn } from "@/shared/lib/utils";

type Status = "healthy" | "running" | "completed" | "failed" | "warning";

const statusClassName: Record<Status, string> = {
  healthy: "border-cyan-400/40 bg-cyan-400/10 text-cyan-200",
  running: "border-emerald-400/40 bg-emerald-400/10 text-emerald-200",
  completed: "border-green-400/40 bg-green-400/10 text-green-200",
  failed: "border-rose-400/40 bg-rose-400/10 text-rose-200",
  warning: "border-amber-400/40 bg-amber-400/10 text-amber-200",
};

interface StatusBadgeProps {
  status: Status;
  children: string;
  className?: string;
}

export function StatusBadge({ status, children, className }: StatusBadgeProps) {
  return (
    <Badge className={cn("border px-2 py-1 uppercase tracking-[0.18em]", statusClassName[status], className)}>
      {children}
    </Badge>
  );
}
