package com.vegawatt.core.notification.domain;

import java.time.Instant;
import java.util.List;

public interface NotificationJobRepository {

    NotificationJob save(NotificationJob job);

    List<NotificationJob> findDue(Instant now, int limit);
}
