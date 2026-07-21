import { useRef, type KeyboardEvent } from "react";

export interface TabItem {
  id: string;
  label: string;
  badge?: { text: string | number; tone?: "neutral" | "danger" };
}

interface TabsProps {
  label: string;
  tabs: TabItem[];
  activeId: string;
  onChange: (id: string) => void;
}

const BADGE_TONE_CLASS: Record<"neutral" | "danger", string> = {
  neutral: "bg-surface-subtle text-text-secondary",
  danger: "bg-danger-soft text-danger",
};

/** An accessible tab bar: click or Left/Right/Home/End to switch, roving tabindex so Tab key
 * moves focus in and out of the whole bar in one stop. Callers render the matching panel
 * (`role="tabpanel"`, `id={panelId(activeId)}`, `aria-labelledby={tabId(activeId)}`) themselves. */
export function Tabs({ label, tabs, activeId, onChange }: TabsProps) {
  const tabRefs = useRef<Record<string, HTMLButtonElement | null>>({});

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const currentIndex = tabs.findIndex((tab) => tab.id === activeId);
    if (currentIndex === -1) return;

    let nextIndex: number | null = null;
    if (event.key === "ArrowRight") nextIndex = (currentIndex + 1) % tabs.length;
    else if (event.key === "ArrowLeft") nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
    else if (event.key === "Home") nextIndex = 0;
    else if (event.key === "End") nextIndex = tabs.length - 1;

    if (nextIndex !== null) {
      event.preventDefault();
      const nextTab = tabs[nextIndex];
      onChange(nextTab.id);
      tabRefs.current[nextTab.id]?.focus();
    }
  }

  return (
    <div
      role="tablist"
      aria-label={label}
      onKeyDown={handleKeyDown}
      className="flex items-center gap-1 overflow-x-auto border-b border-border px-5"
    >
      {tabs.map((tab) => {
        const isActive = tab.id === activeId;
        return (
          <button
            key={tab.id}
            ref={(el) => {
              tabRefs.current[tab.id] = el;
            }}
            type="button"
            role="tab"
            id={tabId(tab.id)}
            aria-selected={isActive}
            aria-controls={panelId(tab.id)}
            tabIndex={isActive ? 0 : -1}
            onClick={() => onChange(tab.id)}
            className={`relative shrink-0 whitespace-nowrap px-3 py-2.5 text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
              isActive ? "text-primary" : "text-text-secondary hover:text-text-primary"
            }`}
          >
            <span className="inline-flex items-center gap-1.5">
              {tab.label}
              {tab.badge && (
                <span
                  className={`tabular-nums rounded-full px-1.5 py-0.5 text-xs font-semibold ${BADGE_TONE_CLASS[tab.badge.tone ?? "neutral"]}`}
                >
                  {tab.badge.text}
                </span>
              )}
            </span>
            {isActive && <span className="absolute inset-x-0 -bottom-px h-0.5 rounded-full bg-primary" aria-hidden="true" />}
          </button>
        );
      })}
    </div>
  );
}

export function tabId(id: string): string {
  return `tab-${id}`;
}

export function panelId(id: string): string {
  return `tabpanel-${id}`;
}
