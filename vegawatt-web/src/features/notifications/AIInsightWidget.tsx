import { useEffect, useState } from "react";
import { Bot, Loader2, Send, Sparkles } from "lucide-react";
import { apiFetch } from "../../shared/api/client";

interface AIInsightWidgetProps {
  homeId?: string;
}

interface AskResponse {
  answer: string;
  fallbackUsed: boolean;
}

const PRESET_QUESTIONS = [
  "Bu ay ne kadar harcadım?",
  "Bu gidişle bütçemi aşar mıyım?",
  "Hangi cihaz en çok tüketiyor?",
  "Tasarruf için ne yapmalıyım?",
];

const LOADING_MESSAGES = [
  "Gemini canlı ev verilerini ve geçmiş tüketimi inceliyor...",
  "Cihaz anomali durumları ve güç seviyeleri taranıyor...",
  "Ay sonu bütçe tahmini ve tasarruf önerileri hesaplanıyor...",
];

export function AIInsightWidget({ homeId }: AIInsightWidgetProps) {
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingMsgIdx, setLoadingMsgIdx] = useState(0);
  const [answer, setAnswer] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!loading) {
      setLoadingMsgIdx(0);
      return;
    }
    const interval = setInterval(() => {
      setLoadingMsgIdx((prev) => (prev + 1) % LOADING_MESSAGES.length);
    }, 2000);
    return () => clearInterval(interval);
  }, [loading]);

  async function handleAsk(promptText: string) {
    if (!promptText.trim() || !homeId) return;
    setLoading(true);
    setAnswer(null);
    setError(null);
    try {
      const res = await apiFetch<AskResponse>(`/api/v1/homes/${homeId}/insights/ask`, {
        method: "POST",
        body: JSON.stringify({ question: promptText }),
      });
      setAnswer(res.answer);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Soru gönderilirken hata oluştu";
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="glow-card rounded-input border border-border bg-surface p-6 shadow-sm">
      <div className="flex items-center gap-2.5">
        <span className="flex h-9 w-9 items-center justify-center rounded-input bg-primary-soft text-primary">
          <Sparkles className="h-5 w-5" aria-hidden="true" />
        </span>
        <div>
          <h2 className="text-base font-semibold text-text-primary">Gemini Enerji Danışmanı</h2>
          <p className="text-xs text-text-muted">Evinizin tüketimi ve bütçeniz hakkında soru sorun</p>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {PRESET_QUESTIONS.map((q) => (
          <button
            key={q}
            type="button"
            onClick={() => {
              setQuestion(q);
              handleAsk(q);
            }}
            disabled={loading || !homeId}
            className="rounded-full border border-border bg-surface-subtle px-3 py-1 text-xs font-medium text-text-secondary transition hover:border-primary hover:text-primary disabled:opacity-50"
          >
            {q}
          </button>
        ))}
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          handleAsk(question);
        }}
        className="mt-4 flex items-center gap-2"
      >
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder={homeId ? "Örn: Ay sonunda faturam ne olur?" : "Lütfen bir ev seçin..."}
          disabled={loading || !homeId}
          className="flex-1 rounded-input border border-border bg-background px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:border-primary focus:outline-none disabled:opacity-60"
        />
        <button
          type="submit"
          disabled={loading || !question.trim() || !homeId}
          className="flex items-center gap-1.5 rounded-input bg-primary px-4 py-2 text-sm font-medium text-white shadow hover:bg-primary-hover disabled:opacity-50 transition"
        >
          {loading ? (
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          ) : (
            <>
              <Send className="h-4 w-4" aria-hidden="true" />
              Sor
            </>
          )}
        </button>
      </form>

      {error && <p className="mt-2 text-xs text-danger">{error}</p>}

      {loading && (
        <div className="mt-4 flex flex-col gap-3 rounded-input border border-primary/30 bg-primary-soft/30 p-4 shadow-sm animate-pulse">
          <div className="flex items-center gap-2.5 text-primary">
            <span className="relative flex h-3 w-3">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75"></span>
              <span className="relative inline-flex h-3 w-3 rounded-full bg-primary"></span>
            </span>
            <Sparkles className="h-4 w-4 animate-spin text-primary" aria-hidden="true" />
            <span className="text-xs font-semibold tracking-wide text-primary">
              {LOADING_MESSAGES[loadingMsgIdx]}
            </span>
          </div>
          <div className="space-y-2">
            <div className="h-3.5 w-3/4 rounded-full bg-primary/20" />
            <div className="h-3.5 w-full rounded-full bg-primary/15" />
            <div className="h-3.5 w-5/6 rounded-full bg-primary/10" />
          </div>
        </div>
      )}

      {answer && !loading && (
        <div className="mt-4 flex items-start gap-3 rounded-input bg-surface-subtle p-4 border border-border">
          <Bot className="h-5 w-5 shrink-0 text-primary mt-0.5" aria-hidden="true" />
          <div className="text-sm text-text-primary leading-relaxed">{answer}</div>
        </div>
      )}
    </div>
  );
}
