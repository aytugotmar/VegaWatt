export type Tone = "primary" | "info" | "warning" | "success" | "danger" | "accent";

/** Shared icon-badge color mapping — reused wherever a small set of stats/features each get
 * their own meaningful accent color (dashboard KPI tiles, landing page feature cards). */
export const TONE_BADGE_CLASSES: Record<Tone, string> = {
  primary: "bg-primary-soft text-primary",
  info: "bg-info-soft text-info",
  warning: "bg-warning-soft text-warning",
  success: "bg-success-soft text-success",
  danger: "bg-danger-soft text-danger",
  accent: "bg-energy-accent/15 text-energy-accent",
};
