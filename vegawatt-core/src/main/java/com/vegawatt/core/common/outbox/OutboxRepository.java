package com.vegawatt.core.common.outbox;

import java.util.List;

public interface OutboxRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findUnpublished(int limit);
}
