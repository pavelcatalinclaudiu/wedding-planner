import { createI18n } from "vue-i18n";
import en from "./locales/en.json";
import ro from "./locales/ro.json";

const savedLang = localStorage.getItem("eternelle-lang");

export const i18n = createI18n({
  legacy: false,
  locale: savedLang ?? "ro",
  fallbackLocale: "en",
  messages: { en, ro },
  datetimeFormats: {
    ro: {
      short: { day: "numeric", month: "short", year: "numeric" },
      long: {
        day: "numeric",
        month: "long",
        year: "numeric",
        weekday: "long",
      },
    },
    en: {
      short: { day: "numeric", month: "short", year: "numeric" },
      long: {
        day: "numeric",
        month: "long",
        year: "numeric",
        weekday: "long",
      },
    },
  },
});

export function setLocale(lang: "ro" | "en") {
  (i18n.global.locale as { value: string }).value = lang;
  localStorage.setItem("eternelle-lang", lang);
  document.documentElement.setAttribute("lang", lang);
}
