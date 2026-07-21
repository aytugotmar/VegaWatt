import {
  CookingPot,
  Droplets,
  Fan,
  Laptop,
  Plug,
  Refrigerator,
  Tv,
  UtensilsCrossed,
  WashingMachine,
  type LucideIcon,
} from "lucide-react";

export interface ApplianceTypePreset {
  key: string;
  label: string;
  type: string;
  icon: LucideIcon;
  safePowerLimitWatt: number;
  simulationMinWatt: number;
  simulationMaxWatt: number;
}

export const APPLIANCE_TYPE_PRESETS: ApplianceTypePreset[] = [
  {
    key: "refrigerator",
    label: "Buzdolabı",
    type: "REFRIGERATOR",
    icon: Refrigerator,
    safePowerLimitWatt: 200,
    simulationMinWatt: 100,
    simulationMaxWatt: 200,
  },
  {
    key: "air_conditioner",
    label: "Klima",
    type: "AIR_CONDITIONER",
    icon: Fan,
    safePowerLimitWatt: 2000,
    simulationMinWatt: 800,
    simulationMaxWatt: 2200,
  },
  {
    key: "washing_machine",
    label: "Çamaşır Makinesi",
    type: "WASHING_MACHINE",
    icon: WashingMachine,
    safePowerLimitWatt: 2200,
    simulationMinWatt: 300,
    simulationMaxWatt: 2300,
  },
  {
    key: "dishwasher",
    label: "Bulaşık Makinesi",
    type: "DISHWASHER",
    icon: UtensilsCrossed,
    safePowerLimitWatt: 1800,
    simulationMinWatt: 200,
    simulationMaxWatt: 1900,
  },
  {
    key: "oven",
    label: "Fırın / Ocak",
    type: "OVEN",
    icon: CookingPot,
    safePowerLimitWatt: 2500,
    simulationMinWatt: 500,
    simulationMaxWatt: 2600,
  },
  {
    key: "water_heater",
    label: "Su Isıtıcısı",
    type: "WATER_HEATER",
    icon: Droplets,
    safePowerLimitWatt: 3000,
    simulationMinWatt: 1000,
    simulationMaxWatt: 3100,
  },
  {
    key: "tv",
    label: "Televizyon",
    type: "TV",
    icon: Tv,
    safePowerLimitWatt: 150,
    simulationMinWatt: 50,
    simulationMaxWatt: 160,
  },
  {
    key: "computer",
    label: "Bilgisayar",
    type: "COMPUTER",
    icon: Laptop,
    safePowerLimitWatt: 300,
    simulationMinWatt: 80,
    simulationMaxWatt: 320,
  },
];

export const CUSTOM_APPLIANCE_KEY = "custom";
const CUSTOM_ICON = Plug;

export function getAppliancePresetByType(type: string | null | undefined): ApplianceTypePreset | undefined {
  if (!type) return undefined;
  return APPLIANCE_TYPE_PRESETS.find((preset) => preset.type === type);
}

export function getApplianceIcon(type: string | null | undefined): LucideIcon {
  return getAppliancePresetByType(type)?.icon ?? CUSTOM_ICON;
}

function titleCaseFallback(type: string): string {
  return type
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

export function getApplianceTypeLabel(type: string | null | undefined): string {
  if (!type) return "Bilinmeyen cihaz";
  return getAppliancePresetByType(type)?.label ?? titleCaseFallback(type);
}
