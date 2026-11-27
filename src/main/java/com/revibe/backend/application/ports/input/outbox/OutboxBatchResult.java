package com.revibe.backend.application.ports.input.outbox;

public record OutboxBatchResult(int processed, int failed) {

    public int total() {
        return processed + failed;
    }
}
