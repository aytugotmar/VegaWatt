package com.vegawatt.core.common.events;

import java.util.List;
import java.util.UUID;

public interface OperationalEventRepository {

    OperationalEvent save(OperationalEvent event);

    List<OperationalEvent> findByHomeId(UUID homeId);
}
