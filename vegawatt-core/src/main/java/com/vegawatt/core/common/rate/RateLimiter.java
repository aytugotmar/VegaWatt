package com.vegawatt.core.common.rate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    // Comfortably larger than any window this app currently rate-limits with (all <= 1 minute) — the
    // sweep only needs to reclaim keys nobody will ever query again, not do the real-time pruning
    // tryAcquire already does for its own key on every call.
    private static final Duration SWEEP_RETENTION = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> requestLogs = new ConcurrentHashMap<>();

    public void tryAcquire(String key, int maxRequests, Duration windowDuration) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(windowDuration);

        requestLogs.compute(key, (k, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayDeque<>();
            }

            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                long retryAfterSeconds = Math.max(1,
                        Duration.between(now, timestamps.peekFirst().plus(windowDuration)).toSeconds());
                throw new RateLimitExceededException("Too many attempts. Please try again later.",
                        retryAfterSeconds);
            }

            timestamps.addLast(now);
            return timestamps;
        });
    }

    // A key that's hit once and never again keeps its (small but non-empty-until-swept) deque in the
    // map forever otherwise, since tryAcquire only ever prunes the key it's called with — a rotating
    // IP/email attacker would grow this map without bound. Runs the same prune logic through
    // computeIfPresent so it stays atomic per-key, same as tryAcquire.
    @Scheduled(fixedDelayString = "PT5M")
    void evictStaleEntries() {
        Instant cutoff = Instant.now().minus(SWEEP_RETENTION);
        int removed = 0;
        for (String key : requestLogs.keySet()) {
            Deque<Instant> remaining = requestLogs.computeIfPresent(key, (k, timestamps) -> {
                while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                    timestamps.pollFirst();
                }
                return timestamps.isEmpty() ? null : timestamps;
            });
            if (remaining == null) {
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Rate limiter swept {} stale key(s)", removed);
        }
    }

    public void clear() {
        requestLogs.clear();
    }
}
