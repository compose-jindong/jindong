import type { I18nConfig } from "fumadocs-core/i18n";

export const i18n: I18nConfig = {
  defaultLanguage: "en",
  languages: ["en", "ko"],
  // Contributors can add: 'zh', 'ja', etc.
  // English: /docs, Others: /ko/docs
  hideLocale: "default-locale",
};

export const languageNames: Record<string, string> = {
  en: "English",
  ko: "한국어",
  zh: "中文",
  ja: "日本語",
};
