CREATE TABLE prescricao (
    id BIGSERIAL PRIMARY KEY,
    atendimento_id BIGINT NOT NULL,
    data_emissao DATE NOT NULL DEFAULT CURRENT_DATE,
    orientacoes_gerais TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP,
    CONSTRAINT fk_prescricao_atendimento FOREIGN KEY (atendimento_id) REFERENCES atendimento(id)
);

CREATE UNIQUE INDEX idx_prescricao_atendimento_ativo
    ON prescricao (atendimento_id)
    WHERE deletado_em IS NULL;