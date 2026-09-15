ALTER TABLE outbox_events DROP COLUMN IF EXISTS correlationid;
ALTER TABLE outbox_events ADD COLUMN correlation_id VARCHAR(250);