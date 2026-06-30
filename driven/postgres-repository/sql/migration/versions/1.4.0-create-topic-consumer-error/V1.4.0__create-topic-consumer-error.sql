CREATE TABLE IF NOT EXISTS topic_consumer_error (
    cod_id          BIGSERIAL       NOT NULL,
    tce_topic_name  VARCHAR(512),
    event_key       TEXT,
    event_payload   TEXT,
    event_offset    INTEGER,
    error           TEXT,
    creation_date   TIMESTAMP,

    CONSTRAINT pk_topic_consumer_error PRIMARY KEY (cod_id)
);

CREATE INDEX IF NOT EXISTS idx_tce_topic_name ON topic_consumer_error (tce_topic_name);
CREATE INDEX IF NOT EXISTS idx_tce_creation_date ON topic_consumer_error (creation_date);
