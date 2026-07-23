import { RefreshCw } from "lucide-react";
import type { ApplianceCatalogItem } from "../../../shared/types/applianceCatalog";
import { ApplianceCatalogCard } from "./ApplianceCatalogCard";

interface ApplianceCatalogGridProps {
  items: ApplianceCatalogItem[];
  hasAnyCatalogItems: boolean;
  loading: boolean;
  error: boolean;
  onRetry: () => void;
  searchQuery: string;
  onAddCustom: () => void;
  onOpenDetails: (item: ApplianceCatalogItem) => void;
  onAdd: (item: ApplianceCatalogItem) => void;
}

export function ApplianceCatalogGrid({
  items,
  hasAnyCatalogItems,
  loading,
  error,
  onRetry,
  searchQuery,
  onAddCustom,
  onOpenDetails,
  onAdd,
}: ApplianceCatalogGridProps) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2" role="status" aria-live="polite">
        {[0, 1, 2, 3].map((index) => (
          <div key={index} className="h-24 animate-pulse rounded-card border border-border bg-surface-subtle" />
        ))}
        <span className="sr-only">Cihaz kataloğu yükleniyor…</span>
      </div>
    );
  }

  if (error) {
    return (
      <div role="alert" className="flex flex-col items-start gap-2 rounded-card border border-danger/30 bg-danger-soft p-4 text-sm text-danger">
        <p>
          Cihaz kataloğu yüklenemedi.
          <br />
          Bağlantıyı kontrol edip yeniden deneyin.
        </p>
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-1.5 rounded-input border border-danger/40 px-2.5 py-1 text-xs font-medium hover:bg-danger/10"
        >
          <RefreshCw className="h-3.5 w-3.5" aria-hidden="true" />
          Yeniden Dene
        </button>
      </div>
    );
  }

  if (!hasAnyCatalogItems) {
    return (
      <p className="rounded-card border border-border bg-surface-subtle p-4 text-sm text-text-secondary">
        Şu anda kullanılabilir cihaz bulunmuyor.
      </p>
    );
  }

  if (items.length === 0) {
    return (
      <div className="rounded-card border border-border bg-surface-subtle p-4 text-sm text-text-secondary">
        <p className="mb-2">
          {searchQuery.trim() ? <>&ldquo;{searchQuery.trim()}&rdquo; ile eşleşen cihaz bulunamadı.</> : "Bu kategoride cihaz bulunamadı."}
        </p>
        <button type="button" onClick={onAddCustom} className="text-sm font-medium text-primary hover:text-primary-hover">
          Özel cihaz ekle
        </button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
      {items.map((item) => (
        <ApplianceCatalogCard
          key={item.id}
          item={item}
          onOpenDetails={() => onOpenDetails(item)}
          onAdd={() => onAdd(item)}
        />
      ))}
    </div>
  );
}
