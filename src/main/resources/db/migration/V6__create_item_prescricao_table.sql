CREATE TABLE item_prescricao (
    id BIGSERIAL PRIMARY KEY,
    prescricao_id BIGINT NOT NULL,
    medicamento_id BIGINT NOT NULL,
    nome_medicamento VARCHAR(100) NOT NULL,   
    dosagem_concentracao VARCHAR(100) NOT NULL,
    posologia VARCHAR(255) NOT NULL,
    duracao_tratamento VARCHAR(50),
    via_administracao VARCHAR(50),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP,
    CONSTRAINT fk_item_prescricao_prescricao FOREIGN KEY (prescricao_id) REFERENCES prescricao(id),
    CONSTRAINT fk_item_prescricao_medicamento FOREIGN KEY (medicamento_id) REFERENCES medicamento(id)
);

CREATE INDEX idx_item_prescricao_prescricao ON item_prescricao (prescricao_id) WHERE deletado_em IS NULL;
CREATE INDEX idx_item_prescricao_medicamento ON item_prescricao (medicamento_id);