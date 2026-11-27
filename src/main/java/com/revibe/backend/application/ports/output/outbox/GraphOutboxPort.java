package com.revibe.backend.application.ports.output.outbox;

import com.revibe.backend.domain.model.OutboxEvent;
import java.util.List;

public interface GraphOutboxPort {
    List<OutboxEvent> findPending(int limit, int maxAttempts);

    OutboxEvent save(OutboxEvent outboxEvent);
}
