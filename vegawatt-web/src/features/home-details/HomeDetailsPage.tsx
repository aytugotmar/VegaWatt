import { ArrowLeft } from "lucide-react";
import { useId } from "react";
import { Link, useParams } from "react-router-dom";
import { useLiveHomeQuery } from "../../shared/hooks/useHomesQueries";
import { HomeDetailsContent } from "./HomeDetailsContent";

export function HomeDetailsPage() {
  const { homeId } = useParams<{ homeId: string }>();
  const titleId = useId();
  const { data: home } = useLiveHomeQuery(homeId);

  if (!homeId) {
    return null;
  }

  return (
    <div className="mx-auto flex max-w-[1400px] flex-col px-8 py-6">
      <Link
        to="/app/homes"
        className="mb-2 inline-flex w-fit items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-text-primary"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Evlerim
      </Link>
      {home && <p className="mb-4 text-sm text-text-muted">{home.appliances.length} cihaz</p>}
      <div className="flex flex-1 flex-col rounded-input border border-border bg-surface">
        <HomeDetailsContent homeId={homeId} titleId={titleId} scrollable={false} />
      </div>
    </div>
  );
}
