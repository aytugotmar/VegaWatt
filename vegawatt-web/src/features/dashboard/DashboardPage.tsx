import { Home as HomeIcon, SearchX, WifiOff } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AddHomeWizard } from "../home-registration/AddHomeWizard";
import { HomeDetailsDialog } from "../home-details/HomeDetailsDialog";
import { EmptyState } from "../../shared/components/EmptyState";
import { Button } from "../../shared/components/Button";
import { HomeCardSkeleton } from "../../shared/components/Skeleton";
import { useToast } from "../../shared/components/ToastProvider";
import { useLiveHomesQuery } from "../../shared/hooks/useHomesQueries";
import type { HomeLiveSummary } from "../../shared/types/home";
import { toSafeNumber } from "../../shared/utils/format";
import { getHomeHealthStatus } from "../../shared/utils/homeStatus";
import { DashboardHeader } from "./DashboardHeader";
import { DashboardKpis } from "./DashboardKpis";
import { HomeFilters, type SortOption, type StatusFilter, type TariffFilter } from "./HomeFilters";
import { HomeTable } from "./HomeTable";
import { MobileHomeCard } from "./MobileHomeCard";
import { PortfolioHealthStrip } from "./PortfolioHealthStrip";

function criticality(home: HomeLiveSummary): number {
  if (home.penaltyActive) return 1000 + toSafeNumber(home.budgetQuotaPercentage);
  return Math.max(toSafeNumber(home.energyQuotaPercentage), toSafeNumber(home.budgetQuotaPercentage));
}

function applyFilters(
  homes: HomeLiveSummary[],
  search: string,
  status: StatusFilter,
  tariff: TariffFilter,
): HomeLiveSummary[] {
  const query = search.trim().toLocaleLowerCase("tr-TR");
  return homes.filter((home) => {
    if (query && !home.homeName.toLocaleLowerCase("tr-TR").includes(query)) return false;
    if (status !== "ALL" && getHomeHealthStatus(home) !== status) return false;
    if (tariff !== "ALL" && home.tariffState !== tariff) return false;
    return true;
  });
}

function applySort(homes: HomeLiveSummary[], sort: SortOption): HomeLiveSummary[] {
  const sorted = [...homes];
  switch (sort) {
    case "CRITICAL":
      return sorted.sort((a, b) => criticality(b) - criticality(a));
    case "COST_DESC":
      return sorted.sort((a, b) => toSafeNumber(b.currentCost) - toSafeNumber(a.currentCost));
    case "ENERGY_DESC":
      return sorted.sort((a, b) => toSafeNumber(b.currentEnergyKwh) - toSafeNumber(a.currentEnergyKwh));
    case "RECENT":
      return sorted.sort((a, b) => new Date(b.lastUpdatedAt).getTime() - new Date(a.lastUpdatedAt).getTime());
    case "NAME":
      return sorted.sort((a, b) => a.homeName.localeCompare(b.homeName, "tr-TR"));
    default:
      return sorted;
  }
}

interface DashboardPageProps {
  mode?: "USER" | "ADMIN";
}

export function DashboardPage({ mode = "ADMIN" }: DashboardPageProps = {}) {
  const { data: homes, isLoading, isError, isFetching, dataUpdatedAt, refetch } = useLiveHomesQuery();
  const [selectedHomeId, setSelectedHomeId] = useState<string | null>(null);
  const [wizardOpen, setWizardOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<StatusFilter>("ALL");
  const [tariff, setTariff] = useState<TariffFilter>("ALL");
  const [sort, setSort] = useState<SortOption>("CRITICAL");
  const { showError } = useToast();
  const navigate = useNavigate();

  function selectHome(homeId: string) {
    if (mode === "USER") {
      navigate(`/app/homes/${homeId}`);
    } else {
      setSelectedHomeId(homeId);
    }
  }

  useEffect(() => {
    if (isError) {
      showError("Ev listesi güncellenirken bağlantı sorunu oluştu. Son bilinen veriler gösteriliyor.");
    }
  }, [isError, showError]);

  const visibleHomes = useMemo(() => {
    if (!homes) return [];
    return applySort(applyFilters(homes, search, status, tariff), sort);
  }, [homes, search, status, tariff, sort]);

  const hasHomes = (homes?.length ?? 0) > 0;
  const hasFiltersApplied = search.trim() !== "" || status !== "ALL" || tariff !== "ALL";

  return (
    <div className="min-h-screen">
      <DashboardHeader
        isConnected={!isError}
        lastUpdatedAt={dataUpdatedAt}
        onAddHome={() => setWizardOpen(true)}
      />

      <main className="mx-auto max-w-6xl px-6 py-6">
        {isError && hasHomes && (
          <div className="mb-4 flex items-center justify-between gap-3 rounded-input border border-danger/30 bg-danger-soft px-4 py-2.5 text-sm text-danger">
            <span>Sunucuyla bağlantı kurulamadı, son bilinen veriler gösteriliyor.</span>
            <Button variant="ghost" onClick={() => refetch()}>
              Tekrar dene
            </Button>
          </div>
        )}

        {isLoading && (
          <div className="grid grid-cols-1 gap-3 sm:hidden">
            {Array.from({ length: 3 }).map((_, index) => (
              <HomeCardSkeleton key={index} />
            ))}
          </div>
        )}
        {isLoading && <HomeTable homes={[]} onSelect={() => {}} loadingRowCount={4} />}

        {!isLoading && hasHomes && (
          <>
            <PortfolioHealthStrip homes={homes ?? []} />
            <DashboardKpis homes={homes ?? []} />
            {isFetching && (
              <p className="mb-2 text-xs text-text-muted" role="status">
                Güncelleniyor…
              </p>
            )}
            <HomeFilters
              search={search}
              onSearchChange={setSearch}
              status={status}
              onStatusChange={setStatus}
              tariff={tariff}
              onTariffChange={setTariff}
              sort={sort}
              onSortChange={setSort}
            />

            {visibleHomes.length === 0 ? (
              <EmptyState
                icon={SearchX}
                title="Sonuç bulunamadı"
                description="Arama veya filtre kriterlerinize uyan bir ev yok."
              />
            ) : (
              <>
                <HomeTable homes={visibleHomes} onSelect={selectHome} />
                <div className="grid grid-cols-1 gap-3 sm:hidden">
                  {visibleHomes.map((home) => (
                    <MobileHomeCard key={home.homeId} home={home} onSelect={selectHome} />
                  ))}
                </div>
              </>
            )}
          </>
        )}

        {!isLoading && isError && !hasHomes && (
          <EmptyState
            icon={WifiOff}
            title="Canlı verilere ulaşılamıyor"
            description="VegaWatt Core şu anda cevap vermiyor. Bağlantı geri geldiğinde panel otomatik yenilenecektir."
            action={
              <Button variant="secondary" onClick={() => refetch()}>
                Tekrar dene
              </Button>
            }
          />
        )}

        {!isLoading && !isError && !hasHomes && !hasFiltersApplied && (
          <EmptyState
            icon={HomeIcon}
            title="Henüz kayıtlı ev yok"
            description="İlk evinizi ekleyerek enerji tüketimini izlemeye başlayın."
            action={
              <Button variant="primary" onClick={() => setWizardOpen(true)}>
                İlk Evi Ekle
              </Button>
            }
          />
        )}
      </main>

      {mode === "ADMIN" && selectedHomeId && (
        <HomeDetailsDialog homeId={selectedHomeId} onClose={() => setSelectedHomeId(null)} />
      )}
      {wizardOpen && <AddHomeWizard onClose={() => setWizardOpen(false)} />}
    </div>
  );
}
