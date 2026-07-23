package com.vegawatt.core.user.infrastructure;

import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
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
    public java.util.List<User> findAll() {
        return jpaRepository.findAll().stream().map(UserRepositoryAdapter::toDomain).toList();
    }

    private static UserEntity toEntity(User user) {
        return new UserEntity(user.id(), user.email(), user.passwordHash(), user.role().name(), user.createdAt());
    }

    private static User toDomain(UserEntity entity) {
        return User.reconstitute(entity.getId(), entity.getEmail(), entity.getPasswordHash(),
                UserRole.valueOf(entity.getRole()), entity.getCreatedAt());
    }
}
