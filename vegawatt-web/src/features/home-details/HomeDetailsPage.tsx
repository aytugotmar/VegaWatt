import { ArrowLeft } from "lucide-react";
import { useId } from "react";
import { Link, useParams } from "react-router-dom";
import { HomeDetailsContent } from "./HomeDetailsContent";

export function HomeDetailsPage() {
  const { homeId } = useParams<{ homeId: string }>();
  const titleId = useId();

  if (!homeId) {
    return null;
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col px-6 py-6">
      <Link
        to="/app/homes"
        className="mb-4 inline-flex w-fit items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-text-primary"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Evlerime dön
      </Link>
      <div className="flex flex-1 flex-col overflow-hidden rounded-input border border-border bg-surface">
        <HomeDetailsContent homeId={homeId} titleId={titleId} />
      </div>
    </div>
  );
}
