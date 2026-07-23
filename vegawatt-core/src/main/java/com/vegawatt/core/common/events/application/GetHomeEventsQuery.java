package com.vegawatt.core.common.events.application;

import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetHomeEventsQuery {

    private final OperationalEventRepository operationalEventRepository;

    public GetHomeEventsQuery(OperationalEventRepository operationalEventRepository) {
        this.operationalEventRepository = operationalEventRepository;
    }

    public List<OperationalEvent> execute(UUID homeId) {
        return operationalEventRepository.findByHomeId(homeId);
    }
}
