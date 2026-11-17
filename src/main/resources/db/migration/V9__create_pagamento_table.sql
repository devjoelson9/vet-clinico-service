
CREATE TABLE pagamento (
    id BIGSERIAL PRIMARY KEY,
    atendimento_id BIGINT NOT NULL,
    valor NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    registrado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pago_em TIMESTAMP,

    CONSTRAINT fk_pagamento_atendimento
        FOREIGN KEY (atendimento_id)
        REFERENCES atendimento(id),

    CONSTRAINT ck_pagamento_valor
        CHECK (valor > 0),

    CONSTRAINT ck_pagamento_status
        CHECK (status IN ('PENDENTE', 'PAGO'))
);

CREATE INDEX idx_pagamento_atendimento
    ON pagamento (atendimento_id);
