import { Home as HomeIcon } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { MobileHomeCard } from "../dashboard/MobileHomeCard";
import { EmptyState } from "../../shared/components/EmptyState";
import { Spinner } from "../../shared/components/Skeleton";
import { AttentionPanel } from "./AttentionPanel";
import { BudgetTrajectory } from "./BudgetTrajectory";
import { LivePowerPulse } from "./LivePowerPulse";
import { TopConsumersPanel } from "./TopConsumersPanel";
import { useOverviewData } from "./useOverviewData";

export function UserOverviewPage() {
  const data = useOverviewData();
  const navigate = useNavigate();

  if (data.isLoading) {
    return (
      <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <Spinner label="Genel bakış yükleniyor..." />
      </div>
    );
  }

  if (data.homeCount === 0) {
    return (
      <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <EmptyState
          icon={HomeIcon}
          title="Henüz kayıtlı ev yok"
          description="İlk evinizi ekleyerek enerji tüketimini izlemeye başlayın."
          action={
            <button type="button" className="btn-primary" onClick={() => navigate("/app/homes")}>
              Evlerime git
            </button>
          }
        />
      </div>
    );
  }

  return (
    <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <h1 className="mb-5 text-2xl font-semibold tracking-tight text-text-primary">Genel Bakış</h1>

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
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Evlerimin Durumu</span>
          <p className="mt-1 text-sm text-text-primary">
            {data.liveCount} canlı{data.staleCount > 0 ? ` · ${data.staleCount} stale` : ""} · {data.homeCount} ev
            toplam
          </p>
        </div>
        <AttentionPanel items={data.attentionItems} />
      </div>

      <div className="mt-4">
        <h2 className="mb-2 text-sm font-semibold text-text-secondary">Evlerim</h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {data.homes.map((home) => (
            <MobileHomeCard key={home.homeId} home={home} onSelect={(homeId) => navigate(`/app/homes/${homeId}`)} />
          ))}
        </div>
      </div>

      <div className="mt-4">
        <TopConsumersPanel consumers={data.topConsumers} />
      </div>
    </div>
  );
}
