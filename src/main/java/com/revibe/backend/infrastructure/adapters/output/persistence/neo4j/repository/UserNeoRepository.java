package com.revibe.backend.infrastructure.adapters.output.persistence.neo4j.repository;

import com.revibe.backend.infrastructure.adapters.output.persistence.neo4j.node.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface UserNeoRepository extends Neo4jRepository<UserNode, String> {
}
