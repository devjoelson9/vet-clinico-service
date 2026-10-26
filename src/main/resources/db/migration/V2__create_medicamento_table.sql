CREATE TABLE medicamento (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    valor_base NUMERIC(10,2) CHECK (valor_base >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP
);

CREATE UNIQUE INDEX idx_medicamento_nome_ativo
    ON medicamento (lower(nome))
    WHERE deletado_em IS NULL;