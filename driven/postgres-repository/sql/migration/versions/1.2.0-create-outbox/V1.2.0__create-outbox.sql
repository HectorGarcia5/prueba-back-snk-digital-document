-- Tabla de Outbox gestionada por fwkcna-starter-outbox-avro-jpa-register.
-- Usada por SNK (publicación inicial) y WEB (republicación).

CREATE SEQUENCE IF NOT EXISTS o_outbox_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS o_outbox (
    cod_n_idoutbox  BIGINT        NOT NULL,
    aggregateid     BYTEA,
    aggregatetype   VARCHAR(255),
    payload         BYTEA,
    fec_dt_creacion TIMESTAMP,
    headers         TEXT,
    CONSTRAINT pk_o_outbox PRIMARY KEY (cod_n_idoutbox)
);

CREATE INDEX IF NOT EXISTS idx_o_outbox_fec
    ON o_outbox (fec_dt_creacion);
