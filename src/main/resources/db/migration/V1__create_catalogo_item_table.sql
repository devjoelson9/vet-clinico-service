CREATE TABLE catalogo_item (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('SERVICO', 'PROCEDIMENTO')),
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    valor_base NUMERIC(10,2) NOT NULL CHECK (valor_base >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP
);

CREATE UNIQUE INDEX idx_catalogo_item_tipo_nome_ativo
    ON catalogo_item (tipo, lower(nome))
    WHERE deletado_em IS NULL;
