CREATE TABLE graph_outbox (
                              id            BIGSERIAL PRIMARY KEY,          -- orden de llegada
                              event_type    VARCHAR(40) NOT NULL,           -- FRIEND_ADDED, EVENT_CREATED...
                              payload       JSONB NOT NULL,                 -- {"a": "...", "b": "..."}
                              created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                              processed_at  TIMESTAMPTZ,                    -- NULL = pendiente
                              attempts      INT NOT NULL DEFAULT 0,
                              last_error    TEXT
);
CREATE INDEX ON graph_outbox (id) WHERE processed_at IS NULL;