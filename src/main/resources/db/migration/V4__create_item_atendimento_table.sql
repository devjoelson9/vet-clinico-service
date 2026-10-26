CREATE TABLE item_atendimento (
    id BIGSERIAL PRIMARY KEY,
    atendimento_id BIGINT NOT NULL,
    catalogo_item_id BIGINT NOT NULL,
    nome_item VARCHAR(100) NOT NULL,
    quantidade NUMERIC(8,2) NOT NULL CHECK (quantidade > 0),
    valor_unitario NUMERIC(10,2) NOT NULL CHECK (valor_unitario >= 0),
    valor_total NUMERIC(10,2) NOT NULL CHECK (valor_total >= 0),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP,
    CONSTRAINT fk_item_atendimento_atendimento FOREIGN KEY (atendimento_id) REFERENCES atendimento(id),
    CONSTRAINT fk_item_atendimento_catalogo FOREIGN KEY (catalogo_item_id) REFERENCES catalogo_item(id)
);

CREATE INDEX idx_item_atendimento_atendimento ON item_atendimento (atendimento_id) WHERE deletado_em IS NULL;
CREATE INDEX idx_item_atendimento_catalogo ON item_atendimento (catalogo_item_id);