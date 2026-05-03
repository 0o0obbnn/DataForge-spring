import { Card, CardContent } from "@/shared/components/ui/card";

interface EmptyStateProps {
  title: string;
  message: string;
}

export function EmptyState({ title, message }: EmptyStateProps) {
  return (
    <Card className="border-slate-800 bg-slate-900">
      <CardContent className="py-10 text-center">
        <h3 className="text-base font-medium text-slate-200">{title}</h3>
        <p className="mt-1 text-sm text-slate-500">{message}</p>
      </CardContent>
    </Card>
  );
}
