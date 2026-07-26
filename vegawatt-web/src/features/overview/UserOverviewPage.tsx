import { useState } from "react";
import { Home as HomeIcon } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { HomeCard } from "../dashboard/HomeCard";
import { HomeTable } from "../dashboard/HomeTable";
import { MobileHomeCard } from "../dashboard/MobileHomeCard";
import { EmptyState } from "../../shared/components/EmptyState";
import { Spinner } from "../../shared/components/Skeleton";
import { ViewModeToggle, type ViewMode } from "../../shared/components/ViewModeToggle";
import { useLanguage } from "../../shared/i18n/LanguageContext";
import { AttentionPanel } from "./AttentionPanel";
import { BudgetTrajectory } from "./BudgetTrajectory";
import { LivePowerPulse } from "./LivePowerPulse";
import { TopConsumersPanel } from "./TopConsumersPanel";
import { useOverviewData } from "./useOverviewData";

export function UserOverviewPage() {
  const { t } = useLanguage();
  const data = useOverviewData();
  const navigate = useNavigate();
  const [viewMode, setViewMode] = useState<ViewMode>("grid");

  if (data.isLoading) {
    return (
      <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <Spinner label={t("overview.loading")} />
      </div>
    );
  }

  if (data.homeCount === 0) {
    return (
      <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <EmptyState
          icon={HomeIcon}
          title={t("overview.noHomesTitle")}
          description={t("overview.noHomesDesc")}
          action={
            <button type="button" className="btn-primary" onClick={() => navigate("/app/homes")}>
              {t("overview.goToHomes")}
            </button>
          }
        />
      </div>
    );
  }

  return (
    <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <h1 className="mb-5 text-2xl font-semibold tracking-tight text-text-primary">{t("overview.title")}</h1>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <LivePowerPulse totalPowerWatt={data.totalPowerWatt} isLoading={data.isLoading} />
        </div>
        <BudgetTrajectory
          totalCurrentCost={data.totalCurrentCost}
          maxBudgetQuotaPercentage={data.maxBudgetQuotaPercentage}
          projectedMonthEndCost={data.projectedMonthEndCost}
        />
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="card-glass flex flex-col justify-center rounded-input border border-border p-5">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">{t("overview.homesStatus")}</span>
          <p className="mt-1 text-sm text-text-primary">
            {data.liveCount} {t("overview.liveCount")}{data.staleCount > 0 ? ` · ${data.staleCount} ${t("overview.staleCount")}` : ""} · {data.homeCount} {t("overview.totalCount")}
          </p>
        </div>
        <AttentionPanel items={data.attentionItems} />
      </div>

      <div className="mt-6">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-text-secondary">{t("nav.homes")}</h2>
          <ViewModeToggle viewMode={viewMode} onViewModeChange={setViewMode} />
        </div>

        {viewMode === "grid" ? (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {data.homes.map((home) => (
              <HomeCard key={home.homeId} home={home} onSelect={(homeId) => navigate(`/app/homes/${homeId}`)} />
            ))}
          </div>
        ) : (
          <>
            <HomeTable homes={data.homes} onSelect={(homeId) => navigate(`/app/homes/${homeId}`)} />
            <div className="grid grid-cols-1 gap-3 sm:hidden">
              {data.homes.map((home) => (
                <MobileHomeCard key={home.homeId} home={home} onSelect={(homeId) => navigate(`/app/homes/${homeId}`)} />
              ))}
            </div>
          </>
        )}
      </div>

      <div className="mt-6">
        <TopConsumersPanel consumers={data.topConsumers} />
      </div>
    </div>
  );
}
