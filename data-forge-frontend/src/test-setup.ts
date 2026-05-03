import i18n from "i18next";
import { initReactI18next } from "react-i18next";

import enCommon from "@/shared/i18n/locales/en/common.json";

i18n.use(initReactI18next).init({
  lng: "en",
  fallbackLng: "en",
  defaultNS: "common",
  ns: ["common"],
  resources: {
    en: { common: enCommon },
  },
  interpolation: { escapeValue: false },
});
