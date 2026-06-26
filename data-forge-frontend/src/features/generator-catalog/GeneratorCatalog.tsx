import { Search, SlidersHorizontal } from "lucide-react";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import { filterGenerators } from "@/features/generator-catalog/catalogFilters";
import { generatorCatalog, mergeWithBackendData, getAllCategories } from "@/features/generator-catalog/generatorCatalogData";
import { useGeneratorsQuery } from "@/features/generator-catalog/generatorQueries";
import { GeneratorDetailDrawer } from "@/features/generator-catalog/GeneratorDetailDrawer";
import { GeneratorDefinition } from "@/features/generator-catalog/generatorCatalogTypes";
import { EmptyState } from "@/shared/components/EmptyState";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Input } from "@/shared/components/ui/input";
import { cn } from "@/shared/lib/utils";

export function GeneratorCatalog() {
  const { t } = useTranslation("common");
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("All");
  const [selectedGenerator, setSelectedGenerator] = useState<GeneratorDefinition>();

  const { data: backendGenerators, isLoading, isError } = useGeneratorsQuery();

  const mergedGenerators = useMemo(
    () => mergeWithBackendData(generatorCatalog, backendGenerators),
    [backendGenerators],
  );

  const generatorCategories = useMemo(
    () => getAllCategories(mergedGenerators),
    [mergedGenerators],
  );

  const filteredGenerators = useMemo(
    () => filterGenerators(mergedGenerators, { query, category }),
    [category, query, mergedGenerators],
  );

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-24 animate-pulse rounded-lg bg-slate-800" />
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-48 animate-pulse rounded-lg bg-slate-800" />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <EmptyState
        title={t("error.loadingFailed", { defaultValue: "加载失败" })}
        message={t("error.generatorsLoadFailed", {
          defaultValue: "无法从服务器获取生成器列表，请稍后重试。",
        })}
      />
    );
  }

  return (
    <div className="space-y-4">
      <Card className="border-slate-800 bg-slate-900">
        <CardContent className="space-y-3 p-4">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-500" />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t("form.searchGenerators")}
              className="border-slate-700 bg-slate-950 pl-9 text-slate-100"
              aria-label={t("form.searchGenerators")}
            />
          </div>

          <div className="flex flex-wrap gap-2" aria-label="Generator categories">
            {generatorCategories.map((item) => (
              <Button
                key={item}
                type="button"
                size="sm"
                variant={category === item ? "default" : "outline"}
                className={cn("rounded-md", category === item ? "" : "border-slate-700")}
                onClick={() => setCategory(item)}
              >
                {t(`catalog.categories.${item}`, { defaultValue: item })}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm text-slate-400">
          <SlidersHorizontal className="size-4" aria-hidden="true" />
          {filteredGenerators.length} {t("nav.catalog")}
        </div>
      </div>

      {filteredGenerators.length > 0 ? (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {filteredGenerators.map((generator) => (
            <Card
              key={generator.id}
              className="border-slate-800 bg-slate-900 text-slate-100 transition hover:border-slate-600 hover:bg-slate-800"
            >
              <CardHeader>
                <div className="flex items-center justify-between gap-3">
                  <Badge className="border-blue-800 bg-blue-950 text-blue-300">
                    {t(`catalog.categories.${generator.category}`, { defaultValue: generator.category })}
                  </Badge>
                  <span className="font-mono text-xs text-slate-500">
                    {generator.params.length} {t("catalog.parameters")}
                  </span>
                </div>
                <CardTitle className="text-lg">
                  {t(`catalog.generators.${generator.id}.name`, { defaultValue: generator.name })}
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="min-h-12 text-sm leading-relaxed text-slate-400">
                  {t(`catalog.generators.${generator.id}.summary`, { defaultValue: generator.summary })}
                </p>
                <code className="block rounded-md border border-slate-800 bg-slate-950 p-2.5 font-mono text-xs text-blue-300">
                  {generator.sample}
                </code>
                <Button className="w-full" variant="outline" onClick={() => setSelectedGenerator(generator)}>
                  {t("actions.viewDetails")}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <EmptyState title={t("empty.noGenerators")} message={t("empty.noGeneratorsMessage")} />
      )}

      <GeneratorDetailDrawer
        generator={selectedGenerator}
        onOpenChange={(open) => !open && setSelectedGenerator(undefined)}
      />
    </div>
  );
}
