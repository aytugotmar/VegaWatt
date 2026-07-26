import { Globe } from "lucide-react";
import { useLanguage } from "../i18n/LanguageContext";

export function LanguageToggle() {
  const { language, setLanguage } = useLanguage();

  return (
    <button
      type="button"
      onClick={() => setLanguage(language === "tr" ? "en" : "tr")}
      className="flex items-center gap-1.5 rounded-full border border-border bg-surface px-2.5 py-1 text-xs font-semibold text-text-secondary transition hover:border-primary hover:text-primary hover:bg-surface-subtle"
      title={language === "tr" ? "Switch to English" : "Türkçeye Geç"}
    >
      <Globe className="h-3.5 w-3.5 shrink-0 text-primary" aria-hidden="true" />
      <span className="uppercase">{language}</span>
    </button>
  );
}
