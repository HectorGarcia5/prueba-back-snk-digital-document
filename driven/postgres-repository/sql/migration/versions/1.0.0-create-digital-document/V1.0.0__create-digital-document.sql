CREATE TABLE IF NOT EXISTS digital_document (
    id                      UUID            NOT NULL,
    employee_id             VARCHAR(50)     NOT NULL,
    managed_group_id        VARCHAR(10)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL,
    failed_step             VARCHAR(20),
    emp_employee_id         VARCHAR(50),
    emp_managed_group_id    VARCHAR(10),
    storage_key             VARCHAR(512),
    checksum                VARCHAR(64),
    retry_count             INTEGER         NOT NULL DEFAULT 0,
    next_retry_at           TIMESTAMPTZ,
    last_error_code         VARCHAR(100),
    last_error_message      VARCHAR(1000),
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    published_at            TIMESTAMPTZ,
    version                 BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_digital_document PRIMARY KEY (id),
    CONSTRAINT uq_digital_document_employee UNIQUE (employee_id, managed_group_id)
);

CREATE INDEX IF NOT EXISTS idx_digital_document_status
    ON digital_document (status);

CREATE INDEX IF NOT EXISTS idx_digital_document_failed_retry
    ON digital_document (status, retry_count, next_retry_at)
    WHERE status = 'FAILED';
