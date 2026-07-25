package com.vegawatt.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.vegawatt.core.session.domain.RefreshSession;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * revokeAllByUserId backs the security-hardening session-revocation flow (password/email/role
 * change): a bug that revoked the wrong user's sessions, or nobody's, would be a silent auth
 * bypass. Runs a real @Modifying JPQL UPDATE against Postgres — mocking the JPA repository
 * would only prove the adapter forwards its arguments, not that the query itself is scoped
 * correctly.
 */
class RefreshSessionRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshSessionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void revokesOnlyTheTargetUsersActiveSessionsLeavingOthersUntouched() {
        UUID targetUserId = insertUser("target@vegawatt.local");
        UUID otherUserId = insertUser("other@vegawatt.local");

        RefreshSession targetSession = repository.save(
                RefreshSession.issue(targetUserId, "target-token-hash", Instant.now().plusSeconds(3600), Instant.now()));
        RefreshSession otherSession = repository.save(
                RefreshSession.issue(otherUserId, "other-token-hash", Instant.now().plusSeconds(3600), Instant.now()));

        repository.revokeAllByUserId(targetUserId, Instant.now());

        RefreshSession reloadedTarget = repository.findByTokenHash(targetSession.tokenHash()).orElseThrow();
        RefreshSession reloadedOther = repository.findByTokenHash(otherSession.tokenHash()).orElseThrow();
        assertThat(reloadedTarget.revokedAt()).isNotNull();
        assertThat(reloadedOther.revokedAt()).isNull();
    }

    @Test
    void revokingTwiceLeavesTheOriginalRevocationTimestampUnchanged() {
        UUID userId = insertUser("idempotent@vegawatt.local");
        RefreshSession session = repository.save(
                RefreshSession.issue(userId, "idempotent-token-hash", Instant.now().plusSeconds(3600), Instant.now()));

        Instant firstRevoke = Instant.now();
        repository.revokeAllByUserId(userId, firstRevoke);
        Instant secondRevoke = firstRevoke.plusSeconds(60);
        repository.revokeAllByUserId(userId, secondRevoke);

        RefreshSession reloaded = repository.findByTokenHash(session.tokenHash()).orElseThrow();
        // The JPQL guards on "revokedAt IS NULL", so a second call must not push the timestamp
        // forward — an already-revoked session's revocation time is a fact, not a moving target.
        assertThat(reloaded.revokedAt()).isCloseTo(firstRevoke, within(1, ChronoUnit.SECONDS));
    }

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, role, created_at)
                VALUES (?, ?, 'hash', 'USER', now())
                """, id, email);
        return id;
    }
}
