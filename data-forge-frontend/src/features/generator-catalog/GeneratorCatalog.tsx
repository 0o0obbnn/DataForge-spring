import { Search, SlidersHorizontal } from "lucide-react";
import { useMemo, useState } from "react";

import { filterGenerators } from "@/features/generator-catalog/catalogFilters";
import { generatorCatalog, generatorCategories } from "@/features/generator-catalog/generatorCatalogData";
import { GeneratorDetailDrawer } from "@/features/generator-catalog/GeneratorDetailDrawer";
import { GeneratorDefinition } from "@/features/generator-catalog/generatorCatalogTypes";
import { EmptyState } from "@/shared/components/EmptyState";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Input } from "@/shared/components/ui/input";
import { cn } from "@/shared/lib/utils";

export function GeneratorCatalog() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("All");
  const [selectedGenerator, setSelectedGenerator] = useState<GeneratorDefinition>();
  const filteredGenerators = useMemo(
    () => filterGenerators(generatorCatalog, { query, category }),
    [category, query],
  );

  return (
    <div className="space-y-6">
      <Card className="border-slate-700/50 bg-slate-950/60 text-slate-50">
        <CardContent className="space-y-4 p-4">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-500" />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by name, category, output, or use case..."
              className="border-slate-700/60 bg-slate-950/70 pl-9 text-slate-50"
              aria-label="Search generators"
            />
          </div>

          <div className="flex flex-wrap gap-2" aria-label="Generator categories">
            {generatorCategories.map((item) => (
              <Button
                key={item}
                type="button"
                size="sm"
                variant={category === item ? "default" : "outline"}
                className={cn(
                  "rounded-full",
                  category === item ? "shadow-[0_0_24px_rgba(34,211,238,0.18)]" : "border-slate-700/60",
                )}
                onClick={() => setCategory(item)}
              >
                {item}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm text-slate-400">
          <SlidersHorizontal className="size-4" aria-hidden="true" />
          {filteredGenerators.length} generators matched
        </div>
      </div>

      {filteredGenerators.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filteredGenerators.map((generator) => (
            <Card
              key={generator.id}
              className="group border-slate-700/50 bg-slate-950/60 text-slate-50 transition hover:border-cyan-300/40 hover:bg-slate-900/70"
            >
              <CardHeader>
                <div className="flex items-center justify-between gap-3">
                  <Badge className="border-cyan-300/30 bg-cyan-300/10 text-cyan-100">{generator.category}</Badge>
                  <span className="font-mono text-xs text-slate-500">{generator.params.length} params</span>
                </div>
                <CardTitle className="text-xl">{generator.name}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="min-h-12 text-sm leading-6 text-slate-400">{generator.summary}</p>
                <code className="block rounded-xl border border-slate-800 bg-slate-950/80 p-3 font-mono text-xs text-cyan-100">
                  {generator.sample}
                </code>
                <Button className="w-full" variant="outline" onClick={() => setSelectedGenerator(generator)}>
                  View Details
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <EmptyState title="No generators found" message="Adjust the search text or switch categories." />
      )}

      <GeneratorDetailDrawer generator={selectedGenerator} onOpenChange={(open) => !open && setSelectedGenerator(undefined)} />
    </div>
  );
}
