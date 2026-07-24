package com.vegawatt.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exercises the SKIP LOCKED claim against a real PostgreSQL, which is the only place its behaviour
 * exists: an in-memory or H2 stand-in would not lock rows the way this depends on. These are the
 * tests that would have caught the duplicate-advisory bug that item 3 fixes.
 */
class NotificationJobClaimIT extends AbstractIntegrationTest {

    private static final int JOB_COUNT = 20;
    private static final int WORKER_THREADS = 4;
    private static final Instant LEASE_UNTIL = Instant.now().plusSeconds(300);

    @Autowired
    private NotificationJobRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentClaimsNeverHandTheSameJobToTwoWorkers() throws InterruptedException {
        Instant dueSince = Instant.now().minusSeconds(1);
        Set<UUID> insertedJobIds = insertDueJobs(JOB_COUNT, dueSince);

        // Every thread races to claim the whole batch at once. With SKIP LOCKED they partition the
        // rows between them; without it they would each read the same unlocked rows and the same job
        // would be handed out more than once.
        Queue<UUID> claimed = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS);
        for (int i = 0; i < WORKER_THREADS; i++) {
            pool.submit(() -> {
                await(start);
                List<NotificationJob> batch;
                do {
                    batch = repository.claimDue(Instant.now(), LEASE_UNTIL, JOB_COUNT);
                    batch.stream().map(NotificationJob::id).filter(insertedJobIds::contains).forEach(claimed::add);
                } while (!batch.isEmpty());
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Every job claimed, and none claimed twice: the list has one entry per job and no repeats.
        assertThat(claimed).hasSize(JOB_COUNT);
        assertThat(Set.copyOf(claimed)).isEqualTo(insertedJobIds);
    }

    @Test
    void aLeasedJobIsNotClaimedAgainWhileTheLeaseHolds() {
        Instant dueSince = Instant.now().minusSeconds(1);
        Set<UUID> insertedJobIds = insertDueJobs(1, dueSince);

        List<NotificationJob> first = repository.claimDue(Instant.now(), LEASE_UNTIL, 10);
        List<NotificationJob> second = repository.claimDue(Instant.now(), LEASE_UNTIL, 10);

        assertThat(first).extracting(NotificationJob::id).containsExactlyElementsOf(insertedJobIds);
        assertThat(second).extracting(NotificationJob::id).doesNotContainAnyElementsOf(insertedJobIds);
    }

    private Set<UUID> insertDueJobs(int count, Instant dueSince) {
        UUID homeId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homes (id, name, contact_email, energy_quota_kwh, budget_quota_try,
                    base_tariff_per_kwh, penalty_tariff_per_kwh, created_at, updated_at)
                VALUES (?, 'Claim Test Home', 'claim@vegawatt.local', 100, 500, 2.5, 5.0, now(), now())
                """, homeId);

        List<UUID> jobIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID eventId = UUID.randomUUID();
            jdbcTemplate.update("""
                    INSERT INTO operational_events (id, home_id, event_type, event_time)
                    VALUES (?, ?, 'ANOMALY', now())
                    """, eventId, homeId);
            NotificationJob job = repository.save(
                    NotificationJob.create(eventId, homeId, null, AdvisoryTriggerType.ANOMALY, dueSince));
            jobIds.add(job.id());
        }
        return jobIds.stream().collect(Collectors.toUnmodifiableSet());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
