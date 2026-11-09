ALTER TABLE item_atendimento ALTER COLUMN catalogo_item_id DROP NOT NULL;

ALTER TABLE item_atendimento ADD COLUMN medicamento_id BIGINT;

ALTER TABLE item_atendimento
    ADD CONSTRAINT fk_item_atendimento_medicamento
        FOREIGN KEY (medicamento_id) REFERENCES medicamento(id);

ALTER TABLE item_atendimento
    ADD CONSTRAINT ck_item_atendimento_origem
        CHECK (
            (catalogo_item_id IS NOT NULL AND medicamento_id IS NULL)
            OR (catalogo_item_id IS NULL AND medicamento_id IS NOT NULL)
        );

CREATE INDEX idx_item_atendimento_medicamento
    ON item_atendimento (medicamento_id)
    WHERE deletado_em IS NULL;
