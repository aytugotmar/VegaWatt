import { Lightbulb } from "lucide-react";
import type { Recommendation } from "../../shared/types/home";
import { EmptyState } from "../../shared/components/EmptyState";
import { RecommendationCard } from "./RecommendationCard";

export function RecommendationsPanel({ recommendations }: { recommendations: Recommendation[] }) {
  if (recommendations.length === 0) {
    return (
      <EmptyState
        icon={Lightbulb}
        title="Henüz öneri yok"
        description="Bu ev için sistem henüz bir öneri oluşturmadı."
      />
    );
  }

  return (
    <ul className="flex flex-col gap-2.5">
      {recommendations.map((recommendation, index) => (
        <RecommendationCard key={recommendation.id} recommendation={recommendation} highlighted={index === 0} />
      ))}
    </ul>
  );
}
