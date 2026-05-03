import { Skeleton } from "@/shared/components/ui/skeleton";

interface LoadingStateProps {
  label?: string;
}

export function LoadingState({ label = "Loading DataForge console data" }: LoadingStateProps) {
  return (
    <div className="space-y-4" role="status" aria-label={label}>
      <Skeleton className="h-8 w-56 bg-slate-700/50" />
      <Skeleton className="h-32 w-full bg-slate-800/70" />
      <Skeleton className="h-32 w-full bg-slate-800/70" />
    </div>
  );
}
