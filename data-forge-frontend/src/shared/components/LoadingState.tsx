import { Loader2 } from "lucide-react";

interface LoadingStateProps {
  label?: string;
}

export function LoadingState({ label }: LoadingStateProps) {
  return (
    <div className="flex items-center gap-2 py-8 text-slate-500">
      <Loader2 className="size-4 animate-spin" aria-hidden="true" />
      <span className="text-sm">{label ?? "加载中..."}</span>
    </div>
  );
}
