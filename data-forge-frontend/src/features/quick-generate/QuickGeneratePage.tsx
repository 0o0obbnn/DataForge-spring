import { useMutation } from "@tanstack/react-query";
import { Play, RotateCcw } from "lucide-react";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import {
  generatorCatalog,
  getAllCategories,
  mergeWithBackendData,
} from "@/features/generator-catalog/generatorCatalogData";
import { useGeneratorsQuery } from "@/features/generator-catalog/generatorQueries";
import { generatePreview, type PreviewRecord } from "@/features/quick-generate/previewApi";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Input } from "@/shared/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/components/ui/table";

const PAGE_SIZE = 10;

export function QuickGeneratePage() {
  const { t } = useTranslation("common");
  const { data: rawBackendGenerators, isLoading: isLoadingGenerators } = useGeneratorsQuery();
  const backendGenerators = Array.isArray(rawBackendGenerators) ? rawBackendGenerators : [];

  const mergedGenerators = useMemo(
    () => mergeWithBackendData(generatorCatalog, backendGenerators),
    [backendGenerators],
  );

  const categories = useMemo(() => getAllCategories(mergedGenerators), [mergedGenerators]);

  const [selectedCategory, setSelectedCategory] = useState("All");
  const [selectedGeneratorId, setSelectedGeneratorId] = useState("");
  const [count, setCount] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);

  const filteredGenerators = useMemo(() => {
    let list =
      selectedCategory === "All"
        ? mergedGenerators
        : mergedGenerators.filter((g) => g.category === selectedCategory);
    return list.sort((a, b) => a.id.localeCompare(b.id));
  }, [mergedGenerators, selectedCategory]);

  const selectedGenerator = useMemo(
    () => mergedGenerators.find((g) => g.id === selectedGeneratorId),
    [mergedGenerators, selectedGeneratorId],
  );

  const previewMutation = useMutation({
    mutationFn: generatePreview,
    onSuccess: () => setCurrentPage(1),
  });

  const records: PreviewRecord[] = previewMutation.data ?? [];

  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(records.length / PAGE_SIZE)),
    [records],
  );

  const paginatedRecords = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return records.slice(start, start + PAGE_SIZE);
  }, [records, currentPage]);

  const handleCategoryChange = (category: string) => {
    setSelectedCategory(category);
    setSelectedGeneratorId("");
  };

  const handleGenerate = () => {
    if (!selectedGeneratorId) return;
    previewMutation.mutate({ generatorType: selectedGeneratorId, count });
  };

  const handleReset = () => {
    setSelectedCategory("All");
    setSelectedGeneratorId("");
    setCount(10);
    previewMutation.reset();
    setCurrentPage(1);
  };

  const handleCountChange = (value: string) => {
    const num = Number(value);
    if (!Number.isNaN(num)) {
      setCount(Math.max(1, Math.min(1000, num)));
    }
  };

  const columnHeader = selectedGenerator
    ? t(`catalog.generators.${selectedGenerator.id}.name`, {
        defaultValue: selectedGenerator.name,
      })
    : t("quickGenerate.valueColumn", { defaultValue: "值" });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-100">
          {t("quickGenerate.title", { defaultValue: "快速生成" })}
        </h1>
        <p className="mt-1 text-sm text-slate-400">
          {t("quickGenerate.subtitle", {
            defaultValue: "选择分类和生成器，设置数量，直接在页面中预览测试数据。",
          })}
        </p>
      </div>

      <Card className="border-slate-800 bg-slate-900">
        <CardHeader>
          <CardTitle className="text-base text-slate-200">
            {t("quickGenerate.configTitle", { defaultValue: "生成配置" })}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-end gap-4">
            <label className="flex min-w-[12rem] flex-1 flex-col gap-2 text-sm">
              <span className="font-medium text-slate-300">
                {t("quickGenerate.categoryLabel", { defaultValue: "分类" })}
              </span>
              <Select value={selectedCategory} onValueChange={handleCategoryChange}>
                <SelectTrigger className="border-slate-700 bg-slate-950 text-slate-100">
                  <SelectValue
                    placeholder={t("quickGenerate.selectCategory", { defaultValue: "选择分类..." })}
                  />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((cat) => (
                    <SelectItem key={cat} value={cat}>
                      {t(`catalog.categories.${cat}`, { defaultValue: cat })}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </label>

            <label className="flex min-w-[16rem] flex-1 flex-col gap-2 text-sm">
              <span className="font-medium text-slate-300">
                {t("quickGenerate.generatorLabel", { defaultValue: "生成器" })}
              </span>
              <Select
                value={selectedGeneratorId}
                onValueChange={setSelectedGeneratorId}
                disabled={isLoadingGenerators || filteredGenerators.length === 0}
              >
                <SelectTrigger className="border-slate-700 bg-slate-950 text-slate-100">
                  <SelectValue
                    placeholder={t("quickGenerate.selectGenerator", { defaultValue: "选择生成器..." })}
                  />
                </SelectTrigger>
                <SelectContent className="max-h-80">
                  {filteredGenerators.map((g) => (
                    <SelectItem key={g.id} value={g.id}>
                      {t(`catalog.generators.${g.id}.name`, { defaultValue: g.name })}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </label>

            <label className="flex w-40 flex-col gap-2 text-sm">
              <span className="font-medium text-slate-300">
                {t("quickGenerate.recordCount", { defaultValue: "生成条数" })}
              </span>
              <Input
                type="number"
                min={1}
                max={1000}
                value={count}
                onChange={(e) => handleCountChange(e.target.value)}
                className="border-slate-700 bg-slate-950"
              />
            </label>

            <Button
              onClick={handleGenerate}
              disabled={!selectedGeneratorId || previewMutation.isPending}
              className="gap-2"
            >
              <Play className="size-4" aria-hidden="true" />
              {previewMutation.isPending
                ? t("quickGenerate.generating", { defaultValue: "生成中..." })
                : t("actions.generate", { defaultValue: "生成数据" })}
            </Button>

            <Button variant="outline" onClick={handleReset} className="gap-2">
              <RotateCcw className="size-4" aria-hidden="true" />
              {t("actions.reset", { defaultValue: "重置" })}
            </Button>
          </div>
        </CardContent>
      </Card>

      {previewMutation.isError && (
        <div className="rounded-lg border border-red-800 bg-red-950/30 p-4 text-sm text-red-400">
          {t("quickGenerate.error", { defaultValue: "生成失败，请检查生成器类型和参数。" })}
        </div>
      )}

      {records.length > 0 && (
        <Card className="border-slate-800 bg-slate-900">
          <CardHeader className="flex-row items-center justify-between">
            <CardTitle className="text-base text-slate-200">
              {t("quickGenerate.resultTitle", { defaultValue: "生成结果" })}{" "}
              <span className="text-sm font-normal text-slate-400">({records.length} 条)</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border border-slate-800">
              <Table>
                <TableHeader>
                  <TableRow className="border-slate-800 hover:bg-transparent">
                    <TableHead className="w-24 text-slate-400">#</TableHead>
                    <TableHead className="text-slate-400">{columnHeader}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedRecords.map((record, index) => {
                    const globalIndex = (currentPage - 1) * PAGE_SIZE + index + 1;
                    return (
                      <TableRow key={globalIndex} className="border-slate-800">
                        <TableCell className="text-slate-500">{globalIndex}</TableCell>
                        <TableCell className="font-mono text-sm text-slate-200">
                          {String(record.value ?? "")}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>

            {totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between">
                <span className="text-sm text-slate-400">
                  {t("quickGenerate.pageInfo", {
                    defaultValue: "第 {{page}} / {{total}} 页",
                    page: currentPage,
                    total: totalPages,
                  })}
                </span>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                    disabled={currentPage <= 1}
                  >
                    {t("quickGenerate.prev", { defaultValue: "上一页" })}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                    disabled={currentPage >= totalPages}
                  >
                    {t("quickGenerate.next", { defaultValue: "下一页" })}
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
