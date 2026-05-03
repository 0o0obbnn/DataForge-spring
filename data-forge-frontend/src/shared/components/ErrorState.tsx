import { AlertCircle } from "lucide-react";

interface ErrorStateProps {
  title: string;
  message: string;
}

export function ErrorState({ title, message }: ErrorStateProps) {
  return (
    <div className="rounded-lg border border-red-900 bg-red-950/30 p-4">
      <div className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 size-5 shrink-0 text-red-400" aria-hidden="true" />
        <div>
          <h3 className="font-medium text-red-300">{title}</h3>
          <p className="mt-1 text-sm text-red-400/80">{message}</p>
        </div>
      </div>
    </div>
  );
}
