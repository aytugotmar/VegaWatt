import { Sparkle } from "lucide-react";
import { Link } from "react-router-dom";

interface BrandMarkProps {
  size?: "sm" | "md";
  tagline?: string;
  className?: string;
  to?: string;
}

const SIZE_CLASSES: Record<NonNullable<BrandMarkProps["size"]>, { badge: string; icon: string; text: string }> = {
  sm: { badge: "h-7 w-7", icon: "h-6 w-6", text: "text-base" },
  md: { badge: "h-8 w-8", icon: "h-7 w-7", text: "text-base" },
};

/** The VegaWatt icon-in-square + wordmark lockup — links to dashboard overview page by default. */
export function BrandMark({ size = "sm", tagline, className = "", to = "/app/overview" }: BrandMarkProps) {
  const { badge, icon, text } = SIZE_CLASSES[size];
  return (
    <Link to={to} className={`flex items-center gap-2.5 transition hover:opacity-90 ${className}`} title="VegaWatt Genel Bakış">
      <span className={`flex ${badge} shrink-0 items-center justify-center text-primary`}>
        <Sparkle className={icon} aria-hidden="true" />
      </span>
      <div>
        <span className={`${text} font-bold tracking-tight text-text-primary`}>VegaWatt</span>
        {tagline && <p className="text-xs leading-tight text-text-secondary">{tagline}</p>}
      </div>
    </Link>
  );
}
