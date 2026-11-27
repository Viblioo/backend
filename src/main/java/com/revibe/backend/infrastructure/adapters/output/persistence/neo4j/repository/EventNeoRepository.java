package com.revibe.backend.infrastructure.adapters.output.persistence.neo4j.repository;

import com.revibe.backend.infrastructure.adapters.output.persistence.neo4j.node.EventNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface EventNeoRepository extends Neo4jRepository<EventNode, String> {
}
