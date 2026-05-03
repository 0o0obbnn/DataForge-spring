import { useTranslation } from "react-i18next";

import { BuilderForm } from "@/features/generator-builder/BuilderForm";

export function GeneratorBuilderPage() {
  const { t } = useTranslation("pages");

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold text-slate-100">{t("builder.title")}</h1>
        <p className="mt-1 text-sm text-slate-400">{t("builder.subtitle")}</p>
      </div>
      <BuilderForm />
    </div>
  );
}
