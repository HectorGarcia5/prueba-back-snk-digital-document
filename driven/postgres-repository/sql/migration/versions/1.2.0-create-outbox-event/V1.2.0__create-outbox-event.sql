CREATE TABLE IF NOT EXISTS outbox_event (
    id                  UUID            NOT NULL,
    aggregate_id        UUID            NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    topic               VARCHAR(200)    NOT NULL,
    event_key           VARCHAR(200)    NOT NULL,
    payload             TEXT            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    publication_reason  VARCHAR(30)     NOT NULL DEFAULT 'INITIAL',
    attempts            INTEGER         NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL,
    published_at        TIMESTAMPTZ,

    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

-- Prevents duplicate INITIAL publications for the same document while
-- allowing explicit DUPLICATE_EVENT and MANUAL_RETRY re-publications.
CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_pending_initial
    ON outbox_event (aggregate_id, event_type)
    WHERE status = 'PENDING' AND publication_reason = 'INITIAL';

CREATE INDEX IF NOT EXISTS idx_outbox_event_pending
    ON outbox_event (status, next_attempt_at)
    WHERE status = 'PENDING';
