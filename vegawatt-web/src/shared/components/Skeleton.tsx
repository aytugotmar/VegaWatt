import { Loader2 } from "lucide-react";

function SkeletonLine({ className = "" }: { className?: string }) {
  return <div className={`skeleton-line h-3 rounded-full ${className}`} />;
}

export function HomeCardSkeleton() {
  return (
    <div
      className="flex flex-col gap-3 rounded-card border border-border bg-surface p-4 shadow-[var(--shadow-card)]"
      data-testid="home-card-skeleton"
      aria-hidden="true"
    >
      <SkeletonLine className="h-5 w-2/3" />
      <SkeletonLine />
      <SkeletonLine />
      <SkeletonLine className="w-2/5" />
    </div>
  );
}

export function TableRowSkeleton({ columns = 8 }: { columns?: number }) {
  return (
    <tr aria-hidden="true" data-testid="table-row-skeleton">
      {Array.from({ length: columns }).map((_, index) => (
        <td key={index} className="px-4 py-3">
          <SkeletonLine className={index === 0 ? "w-28" : "w-16"} />
        </td>
      ))}
    </tr>
  );
}

export function Spinner({ label = "Yükleniyor..." }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 py-6 text-sm text-text-secondary" role="status">
      <Loader2 className="h-4 w-4 animate-spin text-primary" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function AILoadingPulse({ message = "Yanıt hazırlanıyor..." }: { message?: string }) {
  return (
    <div className="flex flex-col gap-3 rounded-input border border-primary/30 bg-primary-soft/30 p-4 shadow-sm animate-pulse" role="status">
      <div className="flex items-center gap-2.5 text-primary">
        <span className="relative flex h-3 w-3">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75" />
          <span className="relative inline-flex h-3 w-3 rounded-full bg-primary" />
        </span>
        <Loader2 className="h-4 w-4 animate-spin text-primary" aria-hidden="true" />
        <span className="text-xs font-semibold tracking-wide text-primary">{message}</span>
      </div>
      <div className="space-y-2">
        <div className="h-3.5 w-3/4 rounded-full bg-primary/20" />
        <div className="h-3.5 w-full rounded-full bg-primary/15" />
        <div className="h-3.5 w-5/6 rounded-full bg-primary/10" />
      </div>
    </div>
  );
}
