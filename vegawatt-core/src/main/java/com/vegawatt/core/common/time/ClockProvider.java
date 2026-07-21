package com.vegawatt.core.common.time;

import java.time.Instant;

public interface ClockProvider {

    Instant now();
}
