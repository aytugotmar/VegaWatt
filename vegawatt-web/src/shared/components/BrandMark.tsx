import { Sparkle } from "lucide-react";

interface BrandMarkProps {
  size?: "sm" | "md";
  tagline?: string;
  className?: string;
}

const SIZE_CLASSES: Record<NonNullable<BrandMarkProps["size"]>, { badge: string; icon: string; text: string }> = {
  sm: { badge: "h-7 w-7", icon: "h-6 w-6", text: "text-base" },
  md: { badge: "h-8 w-8", icon: "h-7 w-7", text: "text-base" },
};

/** The VegaWatt icon-in-square + wordmark lockup — the one shared source for what used to be four
 * independently drifting copies (auth shell, dashboard header, app sidebar, landing page). */
export function BrandMark({ size = "sm", tagline, className = "" }: BrandMarkProps) {
  const { badge, icon, text } = SIZE_CLASSES[size];
  return (
    <div className={`flex items-center gap-2.5 ${className}`}>
      <span className={`flex ${badge} shrink-0 items-center justify-center text-primary`}>
        <Sparkle className={icon} aria-hidden="true" />
      </span>
      <div>
        <span className={`${text} font-bold tracking-tight text-text-primary`}>VegaWatt</span>
        {tagline && <p className="text-xs leading-tight text-text-secondary">{tagline}</p>}
      </div>
    </div>
  );
}
