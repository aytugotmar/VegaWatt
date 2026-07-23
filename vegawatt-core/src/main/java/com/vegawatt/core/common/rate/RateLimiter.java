package com.vegawatt.core.common.rate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

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
                throw new RateLimitExceededException(
                        "Too many attempts. Please try again later.");
            }

            timestamps.addLast(now);
            return timestamps;
        });
    }

    public void clear() {
        requestLogs.clear();
    }
}
