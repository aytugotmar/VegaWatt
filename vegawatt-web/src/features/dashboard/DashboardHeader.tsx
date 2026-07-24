import { Plus } from "lucide-react";
import { BrandMark } from "../../shared/components/BrandMark";
import { Button } from "../../shared/components/Button";
import { ThemeToggle } from "../../shared/components/ThemeToggle";
import { formatRelativeTime } from "../../shared/utils/format";

interface DashboardHeaderProps {
  isConnected: boolean;
  lastUpdatedAt: number | undefined;
  onAddHome: () => void;
}

export function DashboardHeader({ isConnected, lastUpdatedAt, onAddHome }: DashboardHeaderProps) {
  return (
    <header className="sticky top-0 z-30 border-b border-border bg-surface-raised">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-4">
        <BrandMark size="md" tagline="Enerji İzleme Merkezi" />

        <div className="flex items-center gap-3">
          <div className="hidden items-center gap-1.5 text-xs text-text-secondary sm:flex" role="status">
            <span
              className={`h-2 w-2 rounded-full ${isConnected ? "bg-success" : "bg-danger"}`}
              aria-hidden="true"
            />
            <span>
              {isConnected ? "Bağlı" : "Bağlantı kesildi"}
              {lastUpdatedAt ? ` · ${formatRelativeTime(new Date(lastUpdatedAt).toISOString())}` : null}
            </span>
          </div>
          <ThemeToggle />
          <Button variant="primary" onClick={onAddHome}>
            <Plus className="h-4 w-4" aria-hidden="true" />
            Yeni Ev
          </Button>
        </div>
      </div>
    </header>
  );
}
