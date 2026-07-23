export type ApplianceCategory =
  | "KITCHEN"
  | "CLIMATE"
  | "CLEANING"
  | "ENTERTAINMENT"
  | "COMPUTING"
  | "NETWORK_AND_SMART_HOME"
  | "LIGHTING"
  | "PERSONAL_CARE"
  | "OTHER";

export interface ApplianceCatalogItem {
  id: string;
  code: string;
  displayName: string;
  description: string;
  category: ApplianceCategory;
  behaviorProfile: string;
  defaultSafePowerLimitWatt: number;
  defaultActiveMinWatt: number;
  defaultActiveMaxWatt: number;
  defaultStandbyMinWatt: number | null;
  defaultStandbyMaxWatt: number | null;
  supportsStandby: boolean;
  supportsSchedule: boolean;
  supportsOperatingModes: boolean;
  iconKey: string;
  featured: boolean;
  supportedTriggers: string[];
}
