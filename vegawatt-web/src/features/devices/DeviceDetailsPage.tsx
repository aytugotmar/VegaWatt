import { AlertTriangle, ArrowLeft } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApplianceBreachIndicator } from "../home-details/ApplianceBreachIndicator";
import {
  getApplianceDisplayName,
  getApplianceIcon,
  getOperatingModeLabel,
  getOperatingStateLabel,
} from "../../shared/constants/applianceTypes";
import { ApplianceStatusBadge } from "../../shared/components/ApplianceStatusBadge";
import { RadialGauge } from "../../shared/components/RadialGauge";
import { Spinner } from "../../shared/components/Skeleton";
import { formatCurrency, formatEnergy, formatPercentage, formatPower, formatRelativeTime } from "../../shared/utils/format";
import { getApplianceHealthTone } from "../../shared/utils/homeStatus";
import { useDeviceById } from "./useAllAppliances";

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

export function DeviceDetailsPage() {
  const { applianceId } = useParams<{ applianceId: string }>();
  const { device, isLoading } = useDeviceById(applianceId);

  if (!applianceId) return null;

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10">
        <Spinner label="Cihaz bilgileri yükleniyor..." />
      </div>
    );
  }

  if (!device) {
    return (
      <div className="mx-auto max-w-xl px-6 py-16 text-center">
        <div className="rounded-input border border-border bg-surface p-8 shadow-sm">
          <AlertTriangle className="mx-auto h-12 w-12 text-warning" aria-hidden="true" />
          <h2 className="mt-4 text-xl font-semibold text-text-primary">Cihaz Bulunamadı</h2>
          <p className="mt-2 text-sm text-text-muted">
            Aradığınız cihaz sistemde bulunamadı veya erişim yetkiniz yok.
          </p>
          <Link
            to="/app/devices"
            className="mt-6 inline-flex items-center gap-2 rounded-input bg-primary px-4 py-2 text-sm font-medium text-white shadow hover:bg-primary-hover"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            Cihazlara Dön
          </Link>
        </div>
      </div>
    );
  }

  const Icon = getApplianceIcon(device.appliance.applianceType, device.appliance.catalogIconKey);
  const tone = limitBarTone(device.limitUsagePercentage);
  const limitWidth = device.limitUsagePercentage === null ? 0 : Math.min(100, device.limitUsagePercentage);
  const healthTone = getApplianceHealthTone(device.appliance);
  const isLive = healthTone !== "offline" && healthTone !== "stale";
  const operatingStateLabel = getOperatingStateLabel(device.appliance.operatingState);
  const operatingModeLabel = getOperatingModeLabel(device.appliance.operatingMode);

  return (
    <div className="w-full max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8">
      <Link
        to="/app/devices"
        className="mb-4 inline-flex w-fit items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-text-primary"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Cihazlarım
      </Link>

      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-input bg-surface-subtle text-text-secondary">
            <Icon className="h-5 w-5" aria-hidden="true" />
          </span>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight text-text-primary">
              {device.appliance.applianceName}
            </h1>
            <p className="text-sm text-text-muted">
              {device.homeName} ·{" "}
              <Link to={`/app/homes/${device.homeId}`} className="hover:text-primary">
                {getApplianceDisplayName(device.appliance.applianceType, device.appliance.catalogDisplayName)}
              </Link>
            </p>
          </div>
        </div>
        <span className={`flex items-center gap-1.5 text-xs font-medium ${isLive ? "text-success" : "text-danger"}`}>
          <span
            className={`h-1.5 w-1.5 rounded-full ${isLive ? "animate-pulse bg-success" : "bg-danger"}`}
            aria-hidden="true"
          />
          Son ölçüm: {formatRelativeTime(device.appliance.lastUpdatedAt)}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="glow-card flex items-center justify-between gap-4 rounded-input border border-border bg-surface p-6 transition">
          <div>
            <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Canlı Güç</span>
            <p className="glow-text-primary mt-1 text-4xl font-semibold tracking-tight tabular-nums text-text-primary">
              {formatPower(device.appliance.currentPowerWatt)}
            </p>
            <p className="mt-1 text-xs text-text-muted">Toplam tüketimdeki payı</p>
          </div>
          <RadialGauge percentage={device.shareOfTotalPower * 100} />
        </div>

        <div className="rounded-input border border-border bg-surface p-6">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Güvenli Çalışma</span>
          <div className="mt-1 flex items-baseline justify-between">
            <p className="text-2xl font-semibold tabular-nums text-text-primary">
              {formatPower(device.appliance.currentPowerWatt)}
            </p>
            <p className="text-sm text-text-muted">
              / {device.appliance.safePowerLimitWatt === null ? "—" : formatPower(device.appliance.safePowerLimitWatt)}
            </p>
          </div>
          <div className="mt-2 h-2 overflow-hidden rounded-full bg-surface-subtle">
            <div className={`h-full rounded-full transition-all ${BAR_TONE_CLASS[tone]}`} style={{ width: `${limitWidth}%` }} />
          </div>
          {device.limitUsagePercentage !== null && (
            <p className="mt-1.5 text-xs text-text-muted">
              Limit kullanımı: {formatPercentage(device.limitUsagePercentage)}
            </p>
          )}
        </div>
      </div>

      <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <div className="rounded-input border border-border bg-surface p-4">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Bu Ay Enerji</span>
          <p className="mt-1 text-xl font-semibold tabular-nums text-text-primary">
            {formatEnergy(device.accumulatedEnergyKwh)}
          </p>
        </div>
        <div className="rounded-input border border-border bg-surface p-4">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Tahmini Maliyet</span>
          <p className="mt-1 text-xl font-semibold tabular-nums text-text-primary">
            {device.estimatedCost === null ? "—" : formatCurrency(device.estimatedCost)}
          </p>
          <p className="mt-0.5 text-[11px] text-text-muted">Enerji payına göre yaklaşık</p>
        </div>
        <div className="rounded-input border border-border bg-surface p-4">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Tüketimde Pay</span>
          <p className="mt-1 text-xl font-semibold tabular-nums text-text-primary">
            {formatPercentage(device.shareOfTotalPower * 100)}
          </p>
        </div>
        <div className="rounded-input border border-border bg-surface p-4">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Durum</span>
          <div className="mt-1.5 flex items-center gap-2">
            <ApplianceStatusBadge appliance={device.appliance} />
          </div>
          <div className="mt-1.5">
            <ApplianceBreachIndicator consecutiveBreachCount={device.appliance.consecutiveBreachCount} />
          </div>
        </div>
      </div>

      {(operatingStateLabel || operatingModeLabel) && (
        <div className="mt-4 rounded-input border border-border bg-surface p-4">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Çalışma Bilgisi</span>
          <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
            {operatingStateLabel && (
              <>
                <span className="text-text-muted">Çalışma durumu</span>
                <span className="text-right font-medium text-text-primary">{operatingStateLabel}</span>
              </>
            )}
            {operatingModeLabel && (
              <>
                <span className="text-text-muted">Çalışma modu</span>
                <span className="text-right font-medium text-text-primary">{operatingModeLabel}</span>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
