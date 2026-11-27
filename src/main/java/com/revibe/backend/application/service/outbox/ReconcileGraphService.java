package com.revibe.backend.application.service.outbox;

import com.revibe.backend.application.ports.input.outbox.ReconcileGraphUseCase;
import com.revibe.backend.application.ports.output.outbox.GraphSyncPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Red de seguridad del outbox: recorre todos los usuarios ACTIVE y hace MERGE de su nodo. Si el outbox ha funcionado no
 * crea nada; si un evento se agotó, se perdió o se restauró un backup de Neo4j, lo arregla.
 * <p>
 * Sin @Transactional a propósito: son lecturas por páginas y no hace falta bloquear nada mientras se escribe en Neo4j.
 */
@Service
@RequiredArgsConstructor
public class ReconcileGraphService implements ReconcileGraphUseCase {

    static final int PAGE_SIZE = 1000;
    private static final UUID MIN_UUID = new UUID(0L, 0L);

    private final UserRepositoryPort userRepository;
    private final GraphSyncPort graph;

    @Override
    public int reconcileUsers() {
        int created = 0;
        UUID after = MIN_UUID;
        List<UUID> page;

        // Una página llena puede tener más detrás; una incompleta (o vacía) es la última
        do {
            page = userRepository.findActiveIdsAfter(after, PAGE_SIZE);
            created += graph.ensureUsers(page); // con lista vacía no hace nada y devuelve 0
            if (!page.isEmpty()) {
                after = page.getLast();
            }
        } while (page.size() == PAGE_SIZE);

        return created;
    }
}
