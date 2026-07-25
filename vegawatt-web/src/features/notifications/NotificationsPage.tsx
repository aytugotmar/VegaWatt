import { useState } from "react";
import { Bell, AlertTriangle, CheckCircle2 } from "lucide-react";
import { useAllHomesLiveStatus } from "../../shared/hooks/useHomesQueries";
import { formatRelativeTime } from "../../shared/utils/format";

export function NotificationsPage() {
  const { homes, isLoading } = useAllHomesLiveStatus();
  const [selectedHomeId, setSelectedHomeId] = useState<string | undefined>(undefined);

  const activeHome = homes.find((h) => h.homeId === selectedHomeId) ?? homes[0];
  const currentHomeId = activeHome?.homeId;

  return (
    <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8">
      <div className="mb-6 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-text-primary">Bildirimler</h1>
          <p className="text-sm text-text-muted">Tüm evlerinizdeki anomali ve uyarı geçmişini inceleyin</p>
        </div>

        {homes.length > 1 && (
          <select
            value={currentHomeId}
            onChange={(e) => setSelectedHomeId(e.target.value)}
            className="w-full sm:w-auto rounded-input border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:border-primary focus:outline-none"
          >
            {homes.map((h) => (
              <option key={h.homeId} value={h.homeId}>
                {h.homeName}
              </option>
            ))}
          </select>
        )}
      </div>

      <div className="rounded-input border border-border bg-surface p-4 sm:p-6">
        <div className="mb-4 flex items-center justify-between border-b border-border pb-3">
          <h2 className="text-base font-semibold text-text-primary flex items-center gap-2">
            <Bell className="h-4 w-4 text-primary" aria-hidden="true" />
            Canlı Sistem Olayları
          </h2>
          <span className="text-xs text-text-muted">
            {activeHome?.appliances.filter((a) => a.anomalous).length ?? 0} aktif anomali
          </span>
        </div>

        {isLoading ? (
          <p className="text-xs text-text-muted">Olaylar yükleniyor...</p>
        ) : !activeHome ? (
          <p className="text-xs text-text-muted">Kayıtlı ev bulunamadı.</p>
        ) : (
          <div className="flex flex-col gap-3">
            {activeHome.appliances.map((app) => (
              <div
                key={app.applianceId}
                className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 rounded-input border border-border bg-surface-subtle p-3 transition hover:border-border-hover"
              >
                <div className="flex items-center gap-3 min-w-0">
                  {app.anomalous ? (
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-danger-soft text-danger">
                      <AlertTriangle className="h-4 w-4" aria-hidden="true" />
                    </span>
                  ) : (
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-success-soft text-success">
                      <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                    </span>
                  )}
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-text-primary truncate">{app.applianceName}</p>
                    <p className="text-xs text-text-muted">
                      {app.anomalous
                        ? `${app.consecutiveBreachCount} ihlal tespiti — Güç: ${app.currentPowerWatt}W (Limit: ${app.safePowerLimitWatt}W)`
                        : `Normal çalışma — Güç: ${app.currentPowerWatt}W`}
                    </p>
                  </div>
                </div>
                <span className="text-xs text-text-muted shrink-0 self-end sm:self-center">{formatRelativeTime(app.lastUpdatedAt)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
