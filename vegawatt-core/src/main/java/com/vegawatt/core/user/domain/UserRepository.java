package com.vegawatt.core.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAll();

    /** Acquires a transaction-scoped Postgres advisory lock keyed to a fixed admin-count-guard id
     * before counting current admins, serializing this check against every other concurrent call —
     * two concurrent demotions targeting different admins can't both read the same stale "more than
     * one admin left" count and both proceed: the second call blocks until the first transaction
     * commits or rolls back, then counts fresh. */
    int countAdminsUnderGlobalLock();
}
