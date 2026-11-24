-- Estoque integrado (abas Compras, Validades, Perdas e Conferência), no padrão do Anne Gestão:
-- compras sugeridas, validades, perdas e conferência são calculadas na leitura a partir
-- dos movimentos, lotes, fichas e eventos. Grava-se só o que a pessoa informa.

-- Movimento de estoque:
--   validade: validade do que entrou (só em entrada; aparece em Estoque › Validades);
--   unidade_uso: unidade de negócio que usou o item (só em consumo).
ALTER TABLE movimentos_estoque
    ADD COLUMN validade DATE,
    ADD COLUMN unidade_uso VARCHAR(12);

-- Uso de lote de pré-preparo em evento: qual evento. A ordem e o checklist desse evento
-- deixam de baixar os insumos dessa quantidade, para não baixar duas vezes.
ALTER TABLE lote_movimento
    ADD COLUMN evento_id BIGINT REFERENCES producao (id);
CREATE INDEX ix_lote_movimento_evento ON lote_movimento (evento_id);

-- "Pedido feito" da sugestão de compras, um por item. O pedido sai da lista até entrar
-- uma entrada do item com data igual ou posterior à do pedido; desmarcar apaga o registro.
CREATE TABLE compras_pedidos (
    ingrediente_id BIGINT PRIMARY KEY REFERENCES ingredientes (id) ON DELETE CASCADE,
    pedido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    pedido_por VARCHAR(100) NOT NULL DEFAULT ''
);
