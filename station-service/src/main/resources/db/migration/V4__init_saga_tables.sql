CREATE TABLE saga_events(
    id UUID PRIMARY KEY,
    target_id BIGINT NOT NULL ,
    event_type VARCHAR(120) NOT NULL,
    saga_status VARCHAR(60) NOT NULL,
    payload jsonb,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);