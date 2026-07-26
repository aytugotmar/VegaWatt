import { LayoutGrid, List } from "lucide-react";
import { useLanguage } from "../i18n/LanguageContext";

export type ViewMode = "grid" | "list";

interface ViewModeToggleProps {
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
}

export function ViewModeToggle({ viewMode, onViewModeChange }: ViewModeToggleProps) {
  const { t } = useLanguage();

  return (
    <div className="flex items-center gap-1 rounded-lg border border-border bg-surface p-1">
      <button
        type="button"
        onClick={() => onViewModeChange("grid")}
        className={`flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-semibold transition ${
          viewMode === "grid"
            ? "bg-primary text-on-primary shadow-sm"
            : "text-text-secondary hover:text-text-primary"
        }`}
        title={t("view.grid")}
      >
        <LayoutGrid className="h-3.5 w-3.5" aria-hidden="true" />
        <span className="hidden sm:inline">{t("view.grid")}</span>
      </button>
      <button
        type="button"
        onClick={() => onViewModeChange("list")}
        className={`flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-semibold transition ${
          viewMode === "list"
            ? "bg-primary text-on-primary shadow-sm"
            : "text-text-secondary hover:text-text-primary"
        }`}
        title={t("view.list")}
      >
        <List className="h-3.5 w-3.5" aria-hidden="true" />
        <span className="hidden sm:inline">{t("view.list")}</span>
      </button>
    </div>
  );
}
