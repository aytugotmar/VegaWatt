export interface HomeInfoValues {
  name: string;
  contactEmail: string;
}

export interface TargetsValues {
  energyQuotaKwh: string;
  budgetQuotaTry: string;
  baseTariffPerKwh: string;
  penaltyTariffPerKwh: string;
}

export interface PresetSelection {
  checked: boolean;
  quantity: number;
  customized: boolean;
  safePowerLimitWatt: string;
  simulationMinWatt: string;
  simulationMaxWatt: string;
}

export interface CustomAppliance {
  id: number;
  name: string;
  type: string;
  safePowerLimitWatt: string;
  simulationMinWatt: string;
  simulationMaxWatt: string;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export type FieldErrors = Record<string, string>;

export function validateHomeInfo(values: HomeInfoValues): FieldErrors {
  const errors: FieldErrors = {};
  if (!values.name.trim()) errors.name = "Ev adı zorunludur.";
  if (!values.contactEmail.trim()) errors.contactEmail = "E-posta zorunludur.";
  else if (!EMAIL_PATTERN.test(values.contactEmail.trim())) errors.contactEmail = "Geçerli bir e-posta girin.";
  return errors;
}

function positiveNumber(value: string): number | null {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

export function validateTargets(values: TargetsValues): FieldErrors {
  const errors: FieldErrors = {};
  if (positiveNumber(values.energyQuotaKwh) === null) errors.energyQuotaKwh = "Pozitif bir değer girin.";
  if (positiveNumber(values.budgetQuotaTry) === null) errors.budgetQuotaTry = "Pozitif bir değer girin.";

  const base = positiveNumber(values.baseTariffPerKwh);
  const penalty = positiveNumber(values.penaltyTariffPerKwh);
  if (base === null) errors.baseTariffPerKwh = "Pozitif bir değer girin.";
  if (penalty === null) errors.penaltyTariffPerKwh = "Pozitif bir değer girin.";
  if (base !== null && penalty !== null && penalty < base) {
    errors.penaltyTariffPerKwh = "Ceza tarifesi, baz tarifeden düşük olamaz.";
  }
  return errors;
}

export function validateApplianceLimits(values: {
  safePowerLimitWatt: string;
  simulationMinWatt: string;
  simulationMaxWatt: string;
}): FieldErrors {
  const errors: FieldErrors = {};
  const limit = positiveNumber(values.safePowerLimitWatt);
  const min = Number(values.simulationMinWatt);
  const max = positiveNumber(values.simulationMaxWatt);

  if (limit === null) errors.safePowerLimitWatt = "Pozitif bir değer girin.";
  if (!Number.isFinite(min) || min < 0) errors.simulationMinWatt = "0 veya pozitif bir değer girin.";
  if (max === null) errors.simulationMaxWatt = "Pozitif bir değer girin.";
  if (Number.isFinite(min) && max !== null && max <= min) {
    errors.simulationMaxWatt = "Maksimum değer, minimumdan büyük olmalıdır.";
  }
  return errors;
}

export function validateCustomAppliance(appliance: CustomAppliance): FieldErrors {
  const errors = validateApplianceLimits(appliance);
  if (!appliance.name.trim()) errors.name = "Cihaz adı zorunludur.";
  if (!appliance.type.trim()) errors.type = "Cihaz tipi zorunludur.";
  return errors;
}
