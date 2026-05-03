import { DatabaseZap } from "lucide-react";

import { BuilderForm } from "@/features/generator-builder/BuilderForm";

export function GeneratorBuilderPage() {
  return (
    <div className="space-y-6">
      <div>
        <p className="flex items-center gap-2 text-sm uppercase tracking-[0.28em] text-cyan-200">
          <DatabaseZap className="size-4" aria-hidden="true" />
          Generator Builder
        </p>
        <h1 className="mt-3 text-4xl font-semibold tracking-tight">Configure generation jobs</h1>
      </div>
      <BuilderForm />
    </div>
  );
}
