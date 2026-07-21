import { AlertTriangle } from "lucide-react";
import { Button } from "./Button";

interface InlineErrorProps {
  message: string;
  onRetry: () => void;
}

/** A section-scoped "couldn't load" state — distinct from a genuinely empty result, always
 * paired with a retry action so a transient failure never looks like permanent absence of data. */
export function InlineError({ message, onRetry }: InlineErrorProps) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-card border border-border bg-surface-subtle px-4 py-8 text-center">
      <AlertTriangle className="h-5 w-5 text-danger" aria-hidden="true" />
      <p className="text-sm text-text-secondary">{message}</p>
      <Button variant="ghost" onClick={onRetry}>
        Tekrar dene
      </Button>
    </div>
  );
}
