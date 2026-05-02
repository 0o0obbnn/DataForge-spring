import { AlertTriangle, CheckCircle2 } from "lucide-react";

import { generateRequestSchema } from "@/features/generator-builder/builderSchema";
import { BuilderDraft } from "@/features/generator-builder/builderStore";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";

interface RequestPreviewProps {
  draft: BuilderDraft;
}

export function RequestPreview({ draft }: RequestPreviewProps) {
  const validationResult = generateRequestSchema.safeParse(draft);
  const previewJson = JSON.stringify(draft, null, 2);

  return (
    <Card className="sticky top-8 border-slate-700/50 bg-slate-950/70 text-slate-50">
      <CardHeader>
        <CardTitle className="flex items-center justify-between gap-3 text-lg">
          Request Preview
          {validationResult.success ? (
            <span className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.2em] text-emerald-200">
              <CheckCircle2 className="size-4" aria-hidden="true" />
              Valid
            </span>
          ) : (
            <span className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.2em] text-amber-200">
              <AlertTriangle className="size-4" aria-hidden="true" />
              Needs Fix
            </span>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {!validationResult.success ? (
          <div className="rounded-2xl border border-amber-400/30 bg-amber-950/20 p-3 text-sm text-amber-100">
            {validationResult.error.issues.slice(0, 3).map((issue) => (
              <p key={`${issue.path.join(".")}-${issue.message}`}>{issue.message}</p>
            ))}
          </div>
        ) : null}
        <pre className="max-h-[34rem] overflow-auto rounded-2xl border border-slate-800 bg-slate-950/90 p-4 text-xs leading-6 text-cyan-50">
          <code>{previewJson}</code>
        </pre>
      </CardContent>
    </Card>
  );
}
