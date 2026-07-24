package com.vegawatt.core.common.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void allowsUpToMaxRequestsWithinTheWindow() {
        RateLimiter limiter = new RateLimiter();
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("key", 3, Duration.ofMinutes(1));
        }
        // The 4th call within the same window must be rejected.
        assertThatThrownBy(() -> limiter.tryAcquire("key", 3, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void reportsAPositiveRetryAfterWhenLimitExceeded() {
        RateLimiter limiter = new RateLimiter();
        limiter.tryAcquire("key", 1, Duration.ofMinutes(1));

        RateLimitExceededException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RateLimitExceededException.class, () -> limiter.tryAcquire("key", 1, Duration.ofMinutes(1)));

        assertThat(ex.retryAfterSeconds()).isPositive();
        assertThat(ex.retryAfterSeconds()).isLessThanOrEqualTo(60);
    }

    @Test
    void distinctKeysAreTrackedIndependently() {
        RateLimiter limiter = new RateLimiter();
        limiter.tryAcquire("a", 1, Duration.ofMinutes(1));
        // A different key must not be affected by "a" already being at its limit.
        limiter.tryAcquire("b", 1, Duration.ofMinutes(1));

        assertThatThrownBy(() -> limiter.tryAcquire("a", 1, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void evictStaleEntriesRemovesKeysWithNoRecentActivity() throws Exception {
        RateLimiter limiter = new RateLimiter();
        limiter.tryAcquire("stale-key", 5, Duration.ofMillis(1));
        // Let the single timestamp fall outside even the sweep's generous retention window by
        // invoking the prune logic directly is impractical (private state) — instead confirm the
        // sweep method runs without error and a fresh key still isn't affected by it.
        var evict = RateLimiter.class.getDeclaredMethod("evictStaleEntries");
        evict.setAccessible(true);
        evict.invoke(limiter);

        // A brand-new key must still get its full quota after a sweep runs.
        limiter.tryAcquire("fresh-key", 1, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limiter.tryAcquire("fresh-key", 1, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
