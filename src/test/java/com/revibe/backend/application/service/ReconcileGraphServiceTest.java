package com.revibe.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.ports.output.GraphSyncPort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconcileGraphServiceTest {

    private static final UUID MIN = new UUID(0, 0);
    private static final int PAGE = ReconcileGraphService.PAGE_SIZE;

    @Mock
    private UserRepositoryPort users;
    @Mock
    private GraphSyncPort graph;

    private static List<UUID> ids(int n) {
        return Stream.generate(UUID::randomUUID).limit(n).toList();
    }

    @Test
    void walksAllPagesUsingTheLastIdOfEachPage() {
        var page1 = ids(PAGE);
        var page2 = ids(3);
        when(users.findActiveIdsAfter(MIN, PAGE)).thenReturn(page1);
        when(users.findActiveIdsAfter(page1.getLast(), PAGE)).thenReturn(page2);
        when(graph.ensureUsers(page1)).thenReturn(2);
        when(graph.ensureUsers(page2)).thenReturn(1);

        int created = new ReconcileGraphService(users, graph).reconcileUsers();

        assertThat(created).isEqualTo(3);
        verify(users, times(2)).findActiveIdsAfter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void fullLastPageAsksForOneMoreAndStopsWhenEmpty() {
        var page1 = ids(PAGE);
        when(users.findActiveIdsAfter(MIN, PAGE)).thenReturn(page1);
        when(users.findActiveIdsAfter(page1.getLast(), PAGE)).thenReturn(List.of());
        when(graph.ensureUsers(anyList())).thenReturn(0);

        int created = new ReconcileGraphService(users, graph).reconcileUsers();

        assertThat(created).isZero();
        verify(graph, times(2)).ensureUsers(anyList());
    }

    @Test
    void noActiveUsersMakesASingleQuery() {
        when(users.findActiveIdsAfter(MIN, PAGE)).thenReturn(List.of());

        assertThat(new ReconcileGraphService(users, graph).reconcileUsers()).isZero();

        verify(users, times(1)).findActiveIdsAfter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }
}
