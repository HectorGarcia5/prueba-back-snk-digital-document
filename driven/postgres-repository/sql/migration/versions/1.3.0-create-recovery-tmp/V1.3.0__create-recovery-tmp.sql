-- Tabla temporal del batch BTC (prueba-back-btc-digital-document).
-- FWKBatch3 la trunca e inserta en cada iteración del bucle de recuperación.

CREATE TABLE IF NOT EXISTS digital_document_recovery_tmp (
    document_id      UUID         NOT NULL,
    employee_id      VARCHAR(50)  NOT NULL,
    managed_group_id VARCHAR(10)  NOT NULL
);
