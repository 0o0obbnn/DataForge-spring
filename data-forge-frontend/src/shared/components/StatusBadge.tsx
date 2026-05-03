import { cn } from "@/shared/lib/utils";

interface StatusBadgeProps {
  status: "healthy" | "warning" | "error" | "running" | "completed" | "failed";
  children: React.ReactNode;
}

export function StatusBadge({ status, children }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium",
        (status === "healthy" || status === "completed") && "bg-emerald-950 text-emerald-400",
        (status === "warning" || status === "running") && "bg-amber-950 text-amber-400",
        (status === "error" || status === "failed") && "bg-red-950 text-red-400",
      )}
    >
      {children}
    </span>
  );
}
