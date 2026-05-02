import { Boxes } from "lucide-react";

import { GeneratorCatalog } from "@/features/generator-catalog/GeneratorCatalog";

export function GeneratorCatalogPage() {
  return (
    <div className="space-y-6">
      <div>
        <p className="flex items-center gap-2 text-sm uppercase tracking-[0.28em] text-cyan-200">
          <Boxes className="size-4" aria-hidden="true" />
          Generator Catalog
        </p>
        <h1 className="mt-3 text-4xl font-semibold tracking-tight">Discover generator types</h1>
      </div>
      <GeneratorCatalog />
    </div>
  );
}
