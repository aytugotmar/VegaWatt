import { Lightbulb } from "lucide-react";
import type { OperationalEvent, Recommendation } from "../../shared/types/home";
import { EmptyState } from "../../shared/components/EmptyState";
import { RecommendationCard } from "./RecommendationCard";

interface RecommendationsPanelProps {
  recommendations: Recommendation[];
  events: OperationalEvent[];
}

export function RecommendationsPanel({ recommendations, events }: RecommendationsPanelProps) {
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
        <RecommendationCard
          key={recommendation.id}
          recommendation={recommendation}
          highlighted={index === 0}
          linkedEvent={events.find((event) => event.id === recommendation.triggerEventId)}
        />
      ))}
    </ul>
  );
}
