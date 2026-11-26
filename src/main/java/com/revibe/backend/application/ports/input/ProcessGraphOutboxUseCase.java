package com.revibe.backend.application.ports.input;

public interface ProcessGraphOutboxUseCase {
    OutboxBatchResult processPending();
}
