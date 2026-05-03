import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import zhCommon from "@/shared/i18n/locales/zh-CN/common.json";
import zhPages from "@/shared/i18n/locales/zh-CN/pages.json";
import enCommon from "@/shared/i18n/locales/en/common.json";
import enPages from "@/shared/i18n/locales/en/pages.json";

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      "zh-CN": {
        common: zhCommon,
        pages: zhPages,
      },
      en: {
        common: enCommon,
        pages: enPages,
      },
    },
    fallbackLng: "zh-CN",
    defaultNS: "common",
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ["localStorage", "navigator"],
      caches: ["localStorage"],
    },
  });

export default i18n;
