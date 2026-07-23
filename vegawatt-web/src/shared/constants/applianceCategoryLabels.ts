import type { ApplianceCategory } from "../types/applianceCatalog";

export const APPLIANCE_CATEGORY_ORDER: ApplianceCategory[] = [
  "KITCHEN",
  "CLIMATE",
  "CLEANING",
  "ENTERTAINMENT",
  "COMPUTING",
  "NETWORK_AND_SMART_HOME",
  "LIGHTING",
  "PERSONAL_CARE",
  "OTHER",
];

const APPLIANCE_CATEGORY_LABELS: Record<ApplianceCategory, string> = {
  KITCHEN: "Mutfak",
  CLIMATE: "Isıtma ve İklimlendirme",
  CLEANING: "Temizlik ve Bakım",
  ENTERTAINMENT: "Eğlence",
  COMPUTING: "Bilgisayar ve Ofis",
  NETWORK_AND_SMART_HOME: "Ağ ve Akıllı Ev",
  LIGHTING: "Aydınlatma",
  PERSONAL_CARE: "Kişisel Bakım",
  OTHER: "Diğer",
};

export function getApplianceCategoryLabel(category: ApplianceCategory): string {
  return APPLIANCE_CATEGORY_LABELS[category];
}
