import { Volume2, VolumeX, X } from "lucide-react";
import { useId, useMemo, useState } from "react";
import { InlineError } from "../../shared/components/InlineError";
import { Spinner } from "../../shared/components/Skeleton";
import { StatusBadge } from "../../shared/components/StatusBadge";
import { panelId, tabId, Tabs, type TabItem } from "../../shared/components/Tabs";
import {
  readAlertSoundPreference,
  useApplianceAnomalySound,
  writeAlertSoundPreference,
} from "../../shared/hooks/useApplianceAnomalySound";
import { useHomeHistoryQuery, useLiveHomeQuery, useRecommendationsQuery } from "../../shared/hooks/useHomesQueries";
import { getHomeHealthStatus } from "../../shared/utils/homeStatus";
import { formatRelativeTime } from "../../shared/utils/format";
import { ConsumptionChart } from "../history/ConsumptionChart";
import { EnergyHeatmap } from "../history/EnergyHeatmap";
import { computeHistoryRange, HistoryRangeSelector, type HistoryRangeKey } from "../history/HistoryRangeSelector";
import { RecommendationsPanel } from "../recommendations/RecommendationsPanel";
import { AppliancesTable } from "./AppliancesTable";
import { HomeOverviewSection } from "./HomeOverviewSection";

export interface HomeDetailsContentProps {
  homeId: string;
  titleId?: string;
  onClose?: () => void;
}

type DetailTab = "overview" | "devices" | "trends" | "insights";

export function HomeDetailsContent({ homeId, titleId, onClose }: HomeDetailsContentProps) {
  const generatedTitleId = useId();
  const resolvedTitleId = titleId ?? generatedTitleId;
  const [activeTab, setActiveTab] = useState<DetailTab>("overview");
  const [rangeKey, setRangeKey] = useState<HistoryRangeKey>("24H");
  const { from, to } = useMemo(() => computeHistoryRange(rangeKey), [rangeKey]);
  // Fixed at mount so the heatmap always covers a full week regardless of the chart's range
  // selector, and so its query key doesn't change (and refetch) on every render.
  const [heatmapRange] = useState(() => computeHistoryRange("7D"));

  const { data: home, isLoading, isError: homeIsError, refetch: refetchHome } = useLiveHomeQuery(homeId);
  const { data: history, isError: historyIsError, refetch: refetchHistory } = useHomeHistoryQuery(homeId, from, to);
  const { data: heatmapHistory } = useHomeHistoryQuery(homeId, heatmapRange.from, heatmapRange.to);
  const {
    data: recommendations,
    isError: recommendationsIsError,
    refetch: refetchRecommendations,
  } = useRecommendationsQuery(homeId);

  const [soundEnabled, setSoundEnabled] = useState(() => readAlertSoundPreference());
  useApplianceAnomalySound(home?.appliances, soundEnabled);

  function toggleSound() {
    setSoundEnabled((current) => {
      const next = !current;
      writeAlertSoundPreference(next);
      return next;
    });
  }

  const anomalousCount = home?.appliances.filter((appliance) => appliance.anomalous).length ?? 0;
  const applianceCount = home?.appliances.length ?? 0;
  const recommendationCount = recommendations?.length ?? 0;

  const tabs: TabItem[] = [
    { id: "overview", label: "Genel Bakış" },
    {
      id: "devices",
      label: "Cihazlar",
      badge:
        anomalousCount > 0
          ? { text: anomalousCount, tone: "danger" }
          : applianceCount > 0
            ? { text: applianceCount }
            : undefined,
    },
    { id: "trends", label: "Trendler" },
    {
      id: "insights",
      label: "Öneriler",
      badge: recommendationCount > 0 ? { text: recommendationCount } : undefined,
    },
  ];

  return (
    <>
      <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4">
        <div className="flex min-w-0 items-center gap-2">
          {home && (
            <>
              <h2 id={resolvedTitleId} className="truncate text-lg font-semibold text-text-primary">
                {home.homeName}
              </h2>
              <StatusBadge status={getHomeHealthStatus(home)} />
              <span className="hidden text-xs text-text-muted sm:inline">
                {formatRelativeTime(home.lastUpdatedAt)}
              </span>
            </>
          )}
          {!home && (
            <h2 id={resolvedTitleId} className="text-lg font-semibold text-text-primary">
              Ev detayları
            </h2>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <button
            type="button"
            onClick={toggleSound}
            aria-label={soundEnabled ? "Anomali sesini kapat" : "Anomali sesini aç"}
            aria-pressed={soundEnabled}
            title={soundEnabled ? "Anomali sesi açık" : "Anomali sesi kapalı"}
            className="flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition hover:bg-surface-subtle hover:text-text-primary"
          >
            {soundEnabled ? (
              <Volume2 className="h-4 w-4" aria-hidden="true" />
            ) : (
              <VolumeX className="h-4 w-4" aria-hidden="true" />
            )}
          </button>
          {onClose && (
            <button
              type="button"
              onClick={onClose}
              aria-label="Kapat"
              className="flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition hover:bg-surface-subtle hover:text-text-primary"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </button>
          )}
        </div>
      </div>

      {home && (
        <Tabs label="Ev detayı bölümleri" tabs={tabs} activeId={activeTab} onChange={(id) => setActiveTab(id as DetailTab)} />
      )}

      <div className="flex-1 overflow-y-auto px-5 py-4">
        {isLoading && <Spinner label="Ev detayları yükleniyor..." />}

        {!isLoading && homeIsError && !home && (
          <InlineError message="Ev detayları yüklenemedi." onRetry={() => refetchHome()} />
        )}

        {home && (
          <>
            {activeTab === "overview" && (
              <div role="tabpanel" id={panelId("overview")} aria-labelledby={tabId("overview")}>
                <HomeOverviewSection home={home} />
              </div>
            )}

            {activeTab === "devices" && (
              <div role="tabpanel" id={panelId("devices")} aria-labelledby={tabId("devices")}>
                <AppliancesTable appliances={home.appliances} />
              </div>
            )}

            {activeTab === "trends" && (
              <div role="tabpanel" id={panelId("trends")} aria-labelledby={tabId("trends")}>
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <h3 className="text-sm font-semibold uppercase tracking-wide text-text-secondary">
                    Tüketim geçmişi
                  </h3>
                  <HistoryRangeSelector value={rangeKey} onChange={setRangeKey} />
                </div>
                {historyIsError ? (
                  <InlineError message="Geçmiş veriler alınamadı." onRetry={() => refetchHistory()} />
                ) : (
                  <ConsumptionChart points={history ?? []} />
                )}

                <h3 className="mb-3 mt-6 text-sm font-semibold uppercase tracking-wide text-text-secondary">
                  Haftalık Kullanım Deseni
                </h3>
                <EnergyHeatmap points={heatmapHistory ?? []} />
              </div>
            )}

            {activeTab === "insights" && (
              <div role="tabpanel" id={panelId("insights")} aria-labelledby={tabId("insights")}>
                {recommendationsIsError ? (
                  <InlineError message="Öneriler şu anda yüklenemiyor." onRetry={() => refetchRecommendations()} />
                ) : (
                  <RecommendationsPanel recommendations={recommendations ?? []} />
                )}
              </div>
            )}
          </>
        )}
      </div>
    </>
  );
}
