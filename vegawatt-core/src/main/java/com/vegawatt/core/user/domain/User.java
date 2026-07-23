package com.vegawatt.core.user.domain;

import java.time.Instant;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final Instant createdAt;

    private User(UUID id, String email, String passwordHash, UserRole role, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static User register(String email, String passwordHash, Instant createdAt) {
        return new User(UUID.randomUUID(), email, passwordHash, UserRole.USER, createdAt);
    }

    public static User bootstrapAdmin(String email, String passwordHash, Instant createdAt) {
        return new User(UUID.randomUUID(), email, passwordHash, UserRole.ADMIN, createdAt);
    }

    public static User reconstitute(UUID id, String email, String passwordHash, UserRole role, Instant createdAt) {
        return new User(id, email, passwordHash, role, createdAt);
    }

    public User changePassword(String newPasswordHash) {
        return new User(id, email, newPasswordHash, role, createdAt);
    }

    public User changeEmail(String newEmail) {
        return new User(id, newEmail, passwordHash, role, createdAt);
    }

    public User changeRole(UserRole newRole) {
        return new User(id, email, passwordHash, newRole, createdAt);
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserRole role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
