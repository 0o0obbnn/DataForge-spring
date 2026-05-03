import { useTranslation } from "react-i18next";

import { GeneratorCatalog } from "@/features/generator-catalog/GeneratorCatalog";

export function GeneratorCatalogPage() {
  const { t } = useTranslation("pages");

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold text-slate-100">{t("catalog.title")}</h1>
        <p className="mt-1 text-sm text-slate-400">{t("catalog.subtitle")}</p>
      </div>
      <GeneratorCatalog />
    </div>
  );
}
