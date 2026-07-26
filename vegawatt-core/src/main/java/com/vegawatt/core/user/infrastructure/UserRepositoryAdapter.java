package com.vegawatt.core.user.infrastructure;

import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UserRepositoryAdapter implements UserRepository {

    // Arbitrary fixed key for the admin-count guard's advisory lock — any int works as long as
    // it's stable and not reused for an unrelated purpose elsewhere in the schema.
    private static final long ADMIN_COUNT_GUARD_LOCK_KEY = 84_331_001L;

    private final UserJpaRepository jpaRepository;
    private final EntityManager entityManager;

    UserRepositoryAdapter(UserJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(UserRepositoryAdapter::toDomain).toList();
    }

    @Override
    public int countAdminsUnderGlobalLock() {
        // pg_advisory_xact_lock blocks until any other transaction holding this same key commits
        // or rolls back, then auto-releases at this transaction's end — a plain SELECT count(*)
        // taken right after is guaranteed fresh, sidestepping any ambiguity in how a SELECT ...
        // FOR UPDATE's row-recheck-on-lock-wait interacts with a JPQL query targeting a whole
        // predicate-matched set rather than a single known row.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", ADMIN_COUNT_GUARD_LOCK_KEY)
                .getSingleResult();
        return (int) jpaRepository.countByRole(UserRole.ADMIN.name());
    }

    private static UserEntity toEntity(User user) {
        return new UserEntity(user.id(), user.email(), user.passwordHash(), user.role().name(), user.createdAt());
    }

    private static User toDomain(UserEntity entity) {
        return User.reconstitute(entity.getId(), entity.getEmail(), entity.getPasswordHash(),
                UserRole.valueOf(entity.getRole()), entity.getCreatedAt());
    }
}
