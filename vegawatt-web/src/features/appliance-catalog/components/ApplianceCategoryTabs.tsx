import { useRef, type KeyboardEvent } from "react";
import { APPLIANCE_CATEGORY_ORDER, getApplianceCategoryLabel } from "../../../shared/constants/applianceCategoryLabels";
import { FEATURED_TAB, type ApplianceCatalogTab } from "../model/applianceCatalogFilters";

interface Tab {
  key: ApplianceCatalogTab;
  label: string;
}

const TABS: Tab[] = [
  { key: FEATURED_TAB, label: "Sık Kullanılanlar" },
  ...APPLIANCE_CATEGORY_ORDER.map((category) => ({ key: category, label: getApplianceCategoryLabel(category) })),
];

interface ApplianceCategoryTabsProps {
  active: ApplianceCatalogTab;
  onChange: (tab: ApplianceCatalogTab) => void;
}

/** A real ARIA tablist (role="tablist"/"tab", aria-selected, roving tabindex + arrow-key
 * navigation) rather than plain styled buttons — yapılacak.md §25 requires the tabs be usable with
 * a keyboard, and this is the standard accessible pattern for a tab strip. */
export function ApplianceCategoryTabs({ active, onChange }: ApplianceCategoryTabsProps) {
  const buttonRefs = useRef<Array<HTMLButtonElement | null>>([]);

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
    event.preventDefault();
    const delta = event.key === "ArrowRight" ? 1 : -1;
    const nextIndex = (index + delta + TABS.length) % TABS.length;
    onChange(TABS[nextIndex].key);
    buttonRefs.current[nextIndex]?.focus();
  }

  return (
    <div role="tablist" aria-label="Cihaz kategorileri" className="flex gap-1.5 overflow-x-auto pb-1">
      {TABS.map((tab, index) => (
        <button
          key={tab.key}
          ref={(element) => {
            buttonRefs.current[index] = element;
          }}
          type="button"
          role="tab"
          aria-selected={active === tab.key}
          tabIndex={active === tab.key ? 0 : -1}
          onClick={() => onChange(tab.key)}
          onKeyDown={(event) => handleKeyDown(event, index)}
          className={`shrink-0 rounded-full px-3 py-1.5 text-xs font-medium transition ${
            active === tab.key
              ? "bg-primary text-white"
              : "bg-surface-subtle text-text-secondary hover:bg-surface-subtle/80"
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
