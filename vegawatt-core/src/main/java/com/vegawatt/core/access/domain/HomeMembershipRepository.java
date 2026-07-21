package com.vegawatt.core.access.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HomeMembershipRepository {

    HomeMembership save(HomeMembership membership);

    List<HomeMembership> findByUserId(UUID userId);

    Set<UUID> findHomeIdsByUser(UUID userId);
}
