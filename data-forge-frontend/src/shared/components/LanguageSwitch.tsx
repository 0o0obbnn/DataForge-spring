import { useTranslation } from "react-i18next";

export function LanguageSwitch() {
  const { i18n, t } = useTranslation("common");

  const toggleLang = () => {
    const next = i18n.language.startsWith("zh") ? "en" : "zh-CN";
    i18n.changeLanguage(next);
    document.documentElement.lang = next;
  };

  const isZh = i18n.language.startsWith("zh");

  return (
    <button
      type="button"
      onClick={toggleLang}
      className="inline-flex items-center gap-1 rounded-md border border-slate-600 bg-slate-800 px-2 py-1 text-xs text-slate-300 transition hover:border-slate-500 hover:text-slate-100"
      title={t("language.switch")}
    >
      <span className={isZh ? "font-semibold text-white" : "text-slate-500"}>{t("language.zh")}</span>
      <span className="text-slate-600">/</span>
      <span className={!isZh ? "font-semibold text-white" : "text-slate-500"}>{t("language.en")}</span>
    </button>
  );
}
