CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,
                               event_id UUID NOT NULL,
                               exchange VARCHAR(255) NOT NULL,
                               routing_key VARCHAR(255) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                               created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                               CONSTRAINT uq_outbox_events_event_id UNIQUE (event_id)
);

CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);