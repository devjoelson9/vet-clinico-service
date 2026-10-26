CREATE TABLE atendimento (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT,                 
    animal_id BIGINT NOT NULL,
    veterinario_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO'
        CHECK (status IN ('EM_ANDAMENTO', 'FINALIZADO', 'CANCELADO')),
    peso_kg NUMERIC(5,2) CHECK (peso_kg > 0),  
    queixa_sintomas TEXT,
    diagnostico TEXT,
    tratamento TEXT,
    observacoes TEXT,
    orientacoes TEXT,
    iniciado_em TIMESTAMP,
    finalizado_em TIMESTAMP,
    valor_base NUMERIC(10,2) CHECK (valor_base >= 0),
    valor_total NUMERIC(10,2) CHECK (valor_total >= 0), 
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP,
    CONSTRAINT ck_atendimento_finalizado
        CHECK (status <> 'FINALIZADO' OR finalizado_em IS NOT NULL)
);

-- Um agendamento gera no máximo um atendimento ativo
CREATE UNIQUE INDEX idx_atendimento_agendamento_ativo
    ON atendimento (agendamento_id)
    WHERE agendamento_id IS NOT NULL AND deletado_em IS NULL;

CREATE INDEX idx_atendimento_animal ON atendimento (animal_id) WHERE deletado_em IS NULL;
CREATE INDEX idx_atendimento_veterinario ON atendimento (veterinario_id) WHERE deletado_em IS NULL;