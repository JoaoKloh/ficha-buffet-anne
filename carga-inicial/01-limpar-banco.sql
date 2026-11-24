-- Carga inicial do Anne Gestão: apaga os dados de negócio para receber o anne-gestao-carga.json.
--
-- APAGA: clientes, eventos e tudo deles (cardápio, parcelas, extras, etapas, execução da cozinha),
--        fichas técnicas, insumos, estoque, notas fiscais, lançamentos e pagamentos, pratos do
--        restaurante, lotes de pré-preparo, planos de produção, pedidos de compra, escala de equipe,
--        mapa/ordens de produção, logística, orçamento, fechamento e ocorrências do evento
--        e as categorias criadas depois da V2 (id > 23).
-- MANTÉM: usuários e permissões, orçamentos, registros do sistema, histórico do Flyway e as
--         23 categorias semeadas pela V2 (a carga reaproveita 8 delas pelo nome).
--
-- Antes de rodar em produção, faça um pg_dump do banco. Tudo roda numa transação: se a
-- conferência das categorias falhar, nada é apagado.

BEGIN;

DO $$
DECLARE
    esperado TEXT[] := ARRAY[
        'Eventos sociais', 'Eventos corporativos', 'Serviços extras', 'Impostos sobre vendas', 'Descontos e cancelamentos',
        'Alimentos e bebidas (A&B)', 'Mão de obra de evento', 'Descartáveis, embalagens e gás', 'Locação de louças e mobiliário',
        'Transporte e frete', 'Comissões', 'Taxas de cartão e antecipação', 'Degustações', 'Aluguel da cozinha', 'Água, luz e internet',
        'Salários fixos + encargos', 'Pró-labore', 'Contador e sistemas', 'Marketing', 'Manutenção, seguros e alvarás', 'Depreciação',
        'Despesas financeiras', 'IRPJ / CSLL'];
    i INT;
    nome_atual TEXT;
BEGIN
    FOR i IN 1..23 LOOP
        SELECT nome INTO nome_atual FROM categorias_financeiras WHERE id = i;
        IF nome_atual IS DISTINCT FROM esperado[i] THEN
            RAISE EXCEPTION 'Categoria % é "%", esperado "%". O plano de contas não é o da V2; a carga usaria ids errados.',
                i, nome_atual, esperado[i];
        END IF;
    END LOOP;
END $$;

-- Sem CASCADE de propósito: se alguma tabela fora desta lista apontar para elas, o TRUNCATE falha em vez de apagá-la.
TRUNCATE
    evento_ordem_plano, evento_ordem_execucao, evento_liberacao, evento_producao, evento_ocorrencias,
    evento_logistica, evento_orcamento, evento_fechamento, planos_producao_itens, planos_producao, compras_pedidos,
    cozinha_execucoes, evento_etapas, evento_equipe, equipe, lancamento_pagamentos, lancamentos_financeiros, movimentos_estoque,
    nota_fiscal_item, notas_fiscais, parcelas, extras_evento, producao_prato,
    lote_movimento, lotes_preparo, prato_venda_componente, prato_venda_insumo, pratos_venda,
    prato_louca, prato_ingrediente, pratos, producao, clientes, ingredientes
    RESTART IDENTITY;

DELETE FROM categorias_financeiras WHERE id > 23;
ALTER TABLE categorias_financeiras ALTER COLUMN id RESTART WITH 24;

COMMIT;
