import { useId } from "react";
import { Dialog } from "../../shared/components/Dialog";
import { HomeDetailsContent } from "./HomeDetailsContent";

interface HomeDetailsDialogProps {
  homeId: string;
  onClose: () => void;
}

export function HomeDetailsDialog({ homeId, onClose }: HomeDetailsDialogProps) {
  const titleId = useId();

  return (
    <Dialog open onClose={onClose} labelledBy={titleId} maxWidthClassName="max-w-4xl">
      <HomeDetailsContent homeId={homeId} titleId={titleId} onClose={onClose} />
    </Dialog>
  );
}
