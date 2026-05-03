import { Card, CardContent } from "@/shared/components/ui/card";

interface EmptyStateProps {
  title: string;
  message: string;
}

export function EmptyState({ title, message }: EmptyStateProps) {
  return (
    <Card className="border-slate-700/50 bg-slate-900/50">
      <CardContent className="py-12 text-center">
        <h3 className="text-lg font-semibold text-slate-100">{title}</h3>
        <p className="mt-2 text-sm text-slate-400">{message}</p>
      </CardContent>
    </Card>
  );
}
