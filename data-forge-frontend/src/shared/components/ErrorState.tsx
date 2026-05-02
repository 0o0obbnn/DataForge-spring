import { AlertTriangle } from "lucide-react";

import { Card, CardContent } from "@/shared/components/ui/card";

interface ErrorStateProps {
  title: string;
  message: string;
}

export function ErrorState({ title, message }: ErrorStateProps) {
  return (
    <Card className="border-rose-400/30 bg-rose-950/20">
      <CardContent className="flex gap-4 py-6">
        <AlertTriangle className="mt-1 size-5 text-rose-300" aria-hidden="true" />
        <div>
          <h3 className="font-semibold text-rose-100">{title}</h3>
          <p className="mt-1 text-sm text-rose-200/80">{message}</p>
        </div>
      </CardContent>
    </Card>
  );
}
