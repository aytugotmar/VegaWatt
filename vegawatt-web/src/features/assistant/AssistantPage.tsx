import { useState } from "react";
import { useAllHomesLiveStatus } from "../../shared/hooks/useHomesQueries";
import { AIInsightWidget } from "./AIInsightWidget";

export function AssistantPage() {
  const { homes } = useAllHomesLiveStatus();
  const [selectedHomeId, setSelectedHomeId] = useState<string | undefined>(undefined);

  const activeHome = homes.find((h) => h.homeId === selectedHomeId) ?? homes[0];
  const currentHomeId = activeHome?.homeId;

  return (
    <div className="mx-auto max-w-[900px] px-8 py-8">
      <div className="mb-6 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-text-primary">AI Enerji Asistanı</h1>
          <p className="text-sm text-text-muted">Evinizin tüketimi, bütçesi ve tasarruf fırsatları hakkında sorun</p>
        </div>

        {homes.length > 1 && (
          <select
            value={currentHomeId}
            onChange={(e) => setSelectedHomeId(e.target.value)}
            className="rounded-input border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:border-primary focus:outline-none"
          >
            {homes.map((h) => (
              <option key={h.homeId} value={h.homeId}>
                {h.homeName}
              </option>
            ))}
          </select>
        )}
      </div>

      <AIInsightWidget homeId={currentHomeId} />
    </div>
  );
}
