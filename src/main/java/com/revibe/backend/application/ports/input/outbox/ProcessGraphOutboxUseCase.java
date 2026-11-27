package com.revibe.backend.application.ports.input.outbox;

public interface ProcessGraphOutboxUseCase {
    OutboxBatchResult processPending();
}
