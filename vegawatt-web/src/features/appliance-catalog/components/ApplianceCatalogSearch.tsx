import { Search } from "lucide-react";
import type { ChangeEvent } from "react";

interface ApplianceCatalogSearchProps {
  value: string;
  onChange: (value: string) => void;
}

export function ApplianceCatalogSearch({ value, onChange }: ApplianceCatalogSearchProps) {
  return (
    <div className="relative">
      <Search
        className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-text-muted"
        aria-hidden="true"
      />
      <input
        type="search"
        value={value}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onChange(event.target.value)}
        placeholder="Cihaz ara…"
        aria-label="Cihaz ara"
        className="form-input w-full pl-8 text-sm"
      />
    </div>
  );
}
