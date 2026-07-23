import { Cpu } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getApplianceIcon, getApplianceTypeLabel } from "../../shared/constants/applianceTypes";
import { EmptyState } from "../../shared/components/EmptyState";
import { Spinner } from "../../shared/components/Skeleton";
import { formatPercentage, formatPower, formatRelativeTime } from "../../shared/utils/format";
import { useAllAppliances, type DeviceRow } from "./useAllAppliances";

type StatusFilter = "ALL" | "ANOMALOUS" | "NORMAL";

function limitBarTone(percentage: number | null): "normal" | "warning" | "critical" {
  if (percentage === null) return "normal";
  if (percentage >= 100) return "critical";
  if (percentage >= 80) return "warning";
  return "normal";
}

const BAR_TONE_CLASS: Record<"normal" | "warning" | "critical", string> = {
  normal: "bg-primary",
  warning: "bg-warning",
  critical: "bg-danger",
};

export function DevicesPage() {
  const { devices, isLoading, isError } = useAllAppliances();
  const [homeFilter, setHomeFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");

  const homeOptions = useMemo(() => {
    const seen = new Map<string, string>();
    for (const device of devices) seen.set(device.homeId, device.homeName);
    return Array.from(seen.entries());
  }, [devices]);

  const typeOptions = useMemo(() => {
    const seen = new Set<string>();
    for (const device of devices) if (device.appliance.applianceType) seen.add(device.appliance.applianceType);
    return Array.from(seen);
  }, [devices]);

  const filtered = useMemo(() => {
    return devices.filter((device) => {
      if (homeFilter !== "ALL" && device.homeId !== homeFilter) return false;
      if (typeFilter !== "ALL" && device.appliance.applianceType !== typeFilter) return false;
      if (statusFilter === "ANOMALOUS" && !device.appliance.anomalous) return false;
      if (statusFilter === "NORMAL" && device.appliance.anomalous) return false;
      return true;
    });
  }, [devices, homeFilter, typeFilter, statusFilter]);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-[1400px] px-8 py-10">
        <Spinner label="Cihazlar yükleniyor..." />
      </div>
    );
  }

  if (!isError && devices.length === 0) {
    return (
      <div className="mx-auto max-w-[1400px] px-8 py-10">
        <EmptyState icon={Cpu} title="Henüz cihaz yok" description="Evlerinize cihaz ekleyince burada listelenecek." />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-[1400px] px-8 py-8">
      <h1 className="mb-5 text-2xl font-semibold tracking-tight text-text-primary">Cihazlarım</h1>

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <select
          value={homeFilter}
          onChange={(event) => setHomeFilter(event.target.value)}
          className="form-input w-auto"
          aria-label="Ev filtresi"
        >
          <option value="ALL">Tüm evler</option>
          {homeOptions.map(([id, name]) => (
            <option key={id} value={id}>
              {name}
            </option>
          ))}
        </select>
        <select
          value={typeFilter}
          onChange={(event) => setTypeFilter(event.target.value)}
          className="form-input w-auto"
          aria-label="Cihaz tipi filtresi"
        >
          <option value="ALL">Tüm cihaz tipleri</option>
          {typeOptions.map((type) => (
            <option key={type} value={type}>
              {getApplianceTypeLabel(type)}
            </option>
          ))}
        </select>
        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
          className="form-input w-auto"
          aria-label="Durum filtresi"
        >
          <option value="ALL">Tüm durumlar</option>
          <option value="ANOMALOUS">Sadece anomali</option>
          <option value="NORMAL">Sadece normal</option>
        </select>
      </div>

      <div className="overflow-x-auto rounded-card border border-border">
        <table className="w-full min-w-[820px] border-collapse text-sm">
          <thead>
            <tr className="border-b border-border-strong bg-surface-subtle text-left text-xs font-semibold uppercase tracking-wide text-text-secondary">
              <th className="px-4 py-3">Cihaz</th>
              <th className="px-4 py-3">Ev</th>
              <th className="px-4 py-3">Güncel güç</th>
              <th className="px-4 py-3">Limit kullanımı</th>
              <th className="px-4 py-3">Pay</th>
              <th className="px-4 py-3">Son veri</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((device: DeviceRow) => {
              const Icon = getApplianceIcon(device.appliance.applianceType);
              const tone = limitBarTone(device.limitUsagePercentage);
              return (
                <tr
                  key={device.appliance.applianceId}
                  className={`group border-b border-border bg-surface transition last:border-b-0 hover:bg-surface-subtle ${
                    device.appliance.anomalous ? "shadow-[inset_3px_0_0_var(--color-danger)]" : "hover:shadow-[inset_3px_0_0_var(--color-primary)]"
                  }`}
                >
                  <td className="px-4 py-3.5">
                    <Link
                      to={`/app/devices/${device.appliance.applianceId}`}
                      className="flex items-center gap-2.5 group-hover:text-primary"
                    >
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-input bg-surface-subtle text-text-secondary">
                        <Icon className="h-4 w-4" aria-hidden="true" />
                      </span>
                      <div className="flex min-w-0 flex-col">
                        <span className="flex items-center gap-2 truncate font-medium text-text-primary">
                          {device.appliance.applianceName}
                          {device.appliance.anomalous && (
                            <span className="shrink-0 rounded-full bg-danger-soft px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-danger">
                              Yüksek tüketim
                            </span>
                          )}
                        </span>
                        <span className="text-xs text-text-muted">
                          {getApplianceTypeLabel(device.appliance.applianceType)}
                        </span>
                      </div>
                    </Link>
                  </td>
                  <td className="px-4 py-3.5 text-text-secondary">{device.homeName}</td>
                  <td className="tabular-nums px-4 py-3.5 text-text-secondary">
                    {formatPower(device.appliance.currentPowerWatt)}
                  </td>
                  <td className="px-4 py-3.5">
                    {device.limitUsagePercentage === null ? (
                      <span className="text-text-muted">—</span>
                    ) : (
                      <div className="flex items-center gap-2">
                        <div className="h-1.5 w-20 overflow-hidden rounded-full bg-surface-subtle">
                          <div
                            className={`h-full rounded-full ${BAR_TONE_CLASS[tone]}`}
                            style={{ width: `${Math.min(100, device.limitUsagePercentage)}%` }}
                          />
                        </div>
                        <span className="tabular-nums text-xs text-text-secondary">
                          {formatPercentage(device.limitUsagePercentage)}
                        </span>
                      </div>
                    )}
                  </td>
                  <td className="tabular-nums px-4 py-3.5 text-text-muted">
                    {formatPercentage(device.shareOfTotalPower * 100)}
                  </td>
                  <td className="px-4 py-3.5 text-text-muted">{formatRelativeTime(device.appliance.lastUpdatedAt)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
