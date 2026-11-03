CREATE TABLE atenciones (
    id              UUID PRIMARY KEY,
    paciente        VARCHAR(320) NOT NULL,
    prestacion_id   BIGINT       NOT NULL,
    prestacion      VARCHAR(120) NOT NULL,
    fecha           DATE         NOT NULL,
    hora            TIME         NOT NULL,
    estado          VARCHAR(20)  NOT NULL,
    motivo          VARCHAR(255),
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_atenciones_paciente ON atenciones (paciente);
CREATE INDEX idx_atenciones_estado ON atenciones (estado);

CREATE TABLE auditoria (
    id          BIGSERIAL PRIMARY KEY,
    routing_key VARCHAR(120) NOT NULL,
    payload     TEXT         NOT NULL,
    recibido_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
