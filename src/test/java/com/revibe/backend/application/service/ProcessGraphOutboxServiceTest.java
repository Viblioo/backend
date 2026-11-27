package com.revibe.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.ports.output.outbox.GraphOutboxPort;
import com.revibe.backend.application.ports.output.outbox.GraphSyncPort;
import com.revibe.backend.application.ports.output.outbox.GraphUnavailableException;
import com.revibe.backend.application.service.outbox.ProcessGraphOutboxService;
import com.revibe.backend.domain.model.OutboxEvent;
import com.revibe.backend.domain.model.OutboxEventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessGraphOutboxServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

    @Mock
    private GraphOutboxPort outbox;
    @Mock
    private GraphSyncPort graph;

    private ProcessGraphOutboxService service;

    @BeforeEach
    void setUp() {
        service = new ProcessGraphOutboxService(outbox, graph, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static OutboxEvent event(long id) {
        return new OutboxEvent(id, OutboxEventType.USER_REGISTERED, "{}", NOW, null, 0, null);
    }

    private void pending(OutboxEvent... events) {
        when(outbox.findPending(ProcessGraphOutboxService.BATCH_SIZE, ProcessGraphOutboxService.MAX_ATTEMPTS))
                .thenReturn(List.of(events));
    }

    @Test
    void appliedEventIsMarkedProcessed() {
        var e = event(1);
        pending(e);
        when(graph.apply(e)).thenReturn(true);

        var result = service.processPending();

        assertThat(result.processed()).isEqualTo(1);
        assertThat(e.getProcessedAt()).isEqualTo(NOW);
        verify(outbox).save(e);
    }

    @Test
    void unsupportedEventStaysPendingWithoutSpendingAttempts() {
        var e = event(1);
        pending(e);
        when(graph.apply(e)).thenReturn(false);

        var result = service.processPending();

        assertThat(result.total()).isZero();
        assertThat(e.isPending()).isTrue();
        assertThat(e.getAttempts()).isZero();
    }

    @Test
    void brokenEventIsMarkedFailedAndTheRestOfTheBatchContinues() {
        var broken = event(1);
        var ok = event(2);
        pending(broken, ok);
        when(graph.apply(broken)).thenThrow(new IllegalArgumentException("payload roto"));
        when(graph.apply(ok)).thenReturn(true);

        var result = service.processPending();

        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.processed()).isEqualTo(1);
        assertThat(broken.getAttempts()).isEqualTo(1);
        assertThat(broken.getLastError()).contains("payload roto");
        assertThat(broken.isPending()).isTrue();
        assertThat(ok.isPending()).isFalse();
        verify(outbox).save(broken);
        verify(outbox).save(ok);
    }

    @Test
    void neo4jDownStopsTheBatchWithoutSpendingAttempts() {
        var first = event(1);
        var second = event(2);
        pending(first, second);
        when(graph.apply(first)).thenThrow(new GraphUnavailableException("caído", null));

        var result = service.processPending();

        assertThat(result.total()).isZero();
        assertThat(first.getAttempts()).isZero();
        verify(graph, never()).apply(second);
        verify(outbox, never()).save(any());
    }

    @Test
    void eventsProcessedBeforeNeo4jWentDownAreStillSaved() {
        var first = event(1);
        var second = event(2);
        pending(first, second);
        when(graph.apply(first)).thenReturn(true);
        when(graph.apply(second)).thenThrow(new GraphUnavailableException("caído", null));

        var result = service.processPending();

        assertThat(result.processed()).isEqualTo(1);
        verify(outbox).save(first);
        verify(outbox, never()).save(second);
    }
}
