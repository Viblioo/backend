package com.revibe.backend.infrastructure.adapters.output.persistence.postgres;

import com.revibe.backend.application.ports.output.outbox.GraphOutboxPort;
import com.revibe.backend.domain.model.OutboxEvent;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.mapper.OutboxPersistenceMapper;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.repository.OutboxJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements GraphOutboxPort {

    private final OutboxJpaRepository jpaRepository;
    private final OutboxPersistenceMapper mapper;

    /** Bloquea las filas (FOR UPDATE SKIP LOCKED): hay que llamarlo dentro de una transacción. */
    @Override
    public List<OutboxEvent> findPending(int limit, int maxAttempts) {
        return jpaRepository.findPending(limit, maxAttempts).stream().map(mapper::toDomain).toList();
    }

    /**
     * Inserta un evento nuevo (id null) o actualiza uno existente (tras markProcessed / markFailed). Se une a la
     * transacción del caso de uso que lo llama: el evento solo queda guardado si el cambio en PostgreSQL hace commit.
     */
    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(outboxEvent)));
    }
}
