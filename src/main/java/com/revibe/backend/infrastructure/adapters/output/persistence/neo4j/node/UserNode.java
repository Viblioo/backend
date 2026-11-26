package com.revibe.backend.infrastructure.adapters.output.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("User")
public class UserNode {
    @Id
    private String id;
}
