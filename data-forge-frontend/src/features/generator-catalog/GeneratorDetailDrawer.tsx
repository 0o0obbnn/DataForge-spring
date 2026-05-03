import { ArrowRight, Settings2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { useBuilderStore } from "@/features/generator-builder/builderStore";
import { GeneratorDefinition } from "@/features/generator-catalog/generatorCatalogTypes";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/shared/components/ui/sheet";

interface GeneratorDetailDrawerProps {
  generator?: GeneratorDefinition;
  onOpenChange: (open: boolean) => void;
}

export function GeneratorDetailDrawer({ generator, onOpenChange }: GeneratorDetailDrawerProps) {
  const { t } = useTranslation("common");
  const navigate = useNavigate();
  const addField = useBuilderStore((state) => state.addField);

  const handleAddToBuilder = () => {
    if (!generator) {
      return;
    }

    addField(generator.id);
    onOpenChange(false);
    navigate("/builder");
  };

  return (
    <Sheet open={Boolean(generator)} onOpenChange={onOpenChange}>
      <SheetContent className="border-slate-700/60 bg-slate-950 text-slate-50">
        {generator ? (
          <div className="space-y-6">
            <SheetHeader>
              <div className="flex items-center gap-2">
                <Badge className="border-cyan-300/30 bg-cyan-300/10 text-cyan-100">
                  {t(`catalog.categories.${generator.category}`, { defaultValue: generator.category })}
                </Badge>
                <span className="font-mono text-xs text-slate-500">{generator.id}</span>
              </div>
              <SheetTitle className="text-2xl text-slate-50">
                {t(`catalog.generators.${generator.id}.name`, { defaultValue: generator.name })}
              </SheetTitle>
              <SheetDescription className="text-slate-400">
                {t(`catalog.generators.${generator.id}.summary`, { defaultValue: generator.summary })}
              </SheetDescription>
            </SheetHeader>

            <div className="rounded-2xl border border-slate-700/60 bg-slate-900/50 p-4">
              <p className="text-xs uppercase tracking-[0.22em] text-slate-500">{t("catalog.sampleOutput")}</p>
              <code className="mt-3 block break-all font-mono text-sm text-cyan-100">{generator.sample}</code>
            </div>

            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-200">
                <Settings2 className="size-4 text-cyan-200" aria-hidden="true" />
                {t("catalog.parameters")}
              </div>
              {generator.params.length > 0 ? (
                <div className="space-y-2">
                  {generator.params.map((param) => (
                    <div key={param.name} className="rounded-2xl border border-slate-800 bg-slate-900/40 p-3">
                      <div className="flex items-center justify-between gap-3">
                        <span className="font-mono text-sm text-slate-100">{param.name}</span>
                        <Badge variant="outline">{param.type}</Badge>
                      </div>
                      <p className="mt-2 text-sm text-slate-400">
                        {t(`catalog.params.${param.name}`, { defaultValue: param.description })}
                      </p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-slate-500">{t("catalog.noParameters")}</p>
              )}
            </div>

            <Button className="w-full" onClick={handleAddToBuilder}>
              {t("catalog.addToBuilder")}
              <ArrowRight className="size-4" aria-hidden="true" />
            </Button>
          </div>
        ) : null}
      </SheetContent>
    </Sheet>
  );
}
