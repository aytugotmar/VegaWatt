package com.vegawatt.core.access.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "home_memberships")
class HomeMembershipEntity {

    @Id
    private UUID id;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HomeMembershipEntity() {
    }

    HomeMembershipEntity(UUID id, UUID homeId, UUID userId, String role, Instant createdAt) {
        this.id = id;
        this.homeId = homeId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getHomeId() {
        return homeId;
    }

    UUID getUserId() {
        return userId;
    }

    String getRole() {
        return role;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
