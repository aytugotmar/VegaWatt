import { CheckCircle2 } from "lucide-react";
import { Button } from "../../shared/components/Button";
import type { ApplianceRegistration } from "../../shared/types/home";
import type { HomeInfoValues, TargetsValues } from "./registrationSchema";

interface ReviewStepProps {
  homeInfo: HomeInfoValues;
  targets: TargetsValues;
  appliances: ApplianceRegistration[];
  isPending: boolean;
  isSuccess: boolean;
  errorMessage: string | null;
  onSubmit: () => void;
  onClose: () => void;
}

export function ReviewStep({
  homeInfo,
  targets,
  appliances,
  isPending,
  isSuccess,
  errorMessage,
  onSubmit,
  onClose,
}: ReviewStepProps) {
  if (isSuccess) {
    return (
      <div className="flex flex-col items-center gap-3 py-8 text-center">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-success-soft text-success">
          <CheckCircle2 className="h-6 w-6" aria-hidden="true" />
        </span>
        <div>
          <p className="text-base font-semibold text-text-primary">Ev başarıyla kaydedildi</p>
          <p className="mt-1 text-sm text-text-secondary">{homeInfo.name} artık panelde izlenmeye başlandı.</p>
        </div>
        <Button variant="primary" onClick={onClose}>
          Dashboard'a dön
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-input bg-surface-subtle p-3">
        <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-text-secondary">Ev bilgileri</h4>
        <p className="text-sm text-text-primary">{homeInfo.name}</p>
        <p className="text-sm text-text-secondary">{homeInfo.contactEmail}</p>
      </div>

      <div className="rounded-input bg-surface-subtle p-3">
        <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-text-secondary">Hedefler</h4>
        <div className="grid grid-cols-2 gap-2 text-sm text-text-primary">
          <span>Enerji: {targets.energyQuotaKwh} kWh</span>
          <span>Bütçe: {targets.budgetQuotaTry} TRY</span>
          <span>Baz tarife: {targets.baseTariffPerKwh} TRY/kWh</span>
          <span>Ceza tarifesi: {targets.penaltyTariffPerKwh} TRY/kWh</span>
        </div>
      </div>

      <div className="rounded-input bg-surface-subtle p-3">
        <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-text-secondary">
          Cihazlar ({appliances.length})
        </h4>
        <ul className="flex flex-col gap-1 text-sm text-text-primary">
          {appliances.map((appliance, index) => (
            <li key={`${appliance.name}-${index}`} className="flex justify-between gap-2">
              <span>{appliance.name}</span>
              <span className="text-text-muted">{appliance.safePowerLimitWatt} W limit</span>
            </li>
          ))}
        </ul>
      </div>

      {errorMessage && (
        <p className="rounded-input border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          {errorMessage}
        </p>
      )}

      <Button variant="primary" onClick={onSubmit} loading={isPending} disabled={isPending} className="w-fit">
        {isPending ? "Ev kaydediliyor…" : "Evi ve cihazları kaydet"}
      </Button>
    </div>
  );
}
