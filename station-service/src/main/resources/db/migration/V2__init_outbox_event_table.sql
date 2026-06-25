CREATE TABLE outbox_events(
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE ,
    exchange VARCHAR(100) NOT NULL ,
    routing_key VARCHAR(100) NOT NULL ,
    payload TEXT NOT NULL ,
    status VARCHAR(35) NOT NULL ,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status,created_at)