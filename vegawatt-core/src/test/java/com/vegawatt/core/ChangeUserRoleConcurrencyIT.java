package com.vegawatt.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.user.application.ChangeUserRoleUseCase;
import com.vegawatt.core.user.domain.LastAdminDemotionNotAllowedException;
import com.vegawatt.core.user.domain.UserRole;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exercises the last-admin-demotion guard against a real PostgreSQL: with exactly two admins, two
 * concurrent requests each demoting a different one raced past a plain in-memory
 * findAll().stream().filter(ADMIN).count() check, since neither request's read reflected the
 * other's not-yet-committed write — both could see "2 admins left" and both proceed, leaving zero.
 * The fix serializes the check with a Postgres advisory transaction lock before counting, which
 * only a real database's lock-wait-then-commit semantics can prove; a mocked repository would only
 * show the adapter forwards its arguments.
 */
class ChangeUserRoleConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    private ChangeUserRoleUseCase changeUserRoleUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aSingleDemotionActuallyPersistsTheRoleChangeAndNotJustTheSessionRevocation() {
        // Regression test: RefreshSessionJpaRepository's revokeAllByUserId, called right after
        // userRepository.save() in ChangeUserRoleUseCase, used to clear the persistence context
        // without first flushing it — silently discarding the pending role UPDATE and leaving
        // the row unchanged in the database despite execute() returning without error.
        UUID caller = insertUser("caller2@vegawatt.local", UserRole.USER);
        UUID adminA = insertUser("admin-a2@vegawatt.local", UserRole.ADMIN);
        insertUser("admin-b2@vegawatt.local", UserRole.ADMIN);

        changeUserRoleUseCase.execute(caller, adminA, UserRole.USER);

        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, adminA);
        assertThat(role).isEqualTo("USER");
    }

    @Test
    void concurrentDemotionsOfTheTwoLastAdminsNeverLeaveZeroAdmins() throws Exception {
        UUID caller = insertUser("caller@vegawatt.local", UserRole.USER);
        UUID adminA = insertUser("admin-a@vegawatt.local", UserRole.ADMIN);
        UUID adminB = insertUser("admin-b@vegawatt.local", UserRole.ADMIN);

        AtomicInteger rejections = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<?> demoteA = pool.submit(() -> race(start, caller, adminA, rejections, successes));
        Future<?> demoteB = pool.submit(() -> race(start, caller, adminB, rejections, successes));

        start.countDown();
        demoteA.get(30, TimeUnit.SECONDS);
        demoteB.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Started with exactly two admins: one demotion must win, the other must be rejected,
        // leaving exactly one admin — never zero.
        int remainingAdmins = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE role = 'ADMIN'", Integer.class);
        assertThat(remainingAdmins).isEqualTo(1);
        assertThat(successes.get()).isEqualTo(1);
        assertThat(rejections.get()).isEqualTo(1);
    }

    private void race(CountDownLatch start, UUID caller, UUID target, AtomicInteger rejections,
                       AtomicInteger successes) {
        try {
            start.await();
            changeUserRoleUseCase.execute(caller, target, UserRole.USER);
            successes.incrementAndGet();
        } catch (LastAdminDemotionNotAllowedException e) {
            rejections.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private UUID insertUser(String email, UserRole role) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, role, created_at)
                VALUES (?, ?, 'hash', ?, now())
                """, id, email, role.name());
        return id;
    }
}
