-- Função e diária padrão da pessoa da equipe: usadas quando ela é escalada em lote (checklist
-- da escala) e ajustáveis depois em cada evento. Quem já foi escalado herda a função e a
-- diária da escala mais recente.
ALTER TABLE equipe
    ADD COLUMN funcao_padrao VARCHAR(30) NOT NULL DEFAULT 'Garçom',
    ADD COLUMN diaria_padrao NUMERIC(15, 2) NOT NULL DEFAULT 0;

UPDATE equipe e
SET funcao_padrao = x.funcao, diaria_padrao = x.valor
FROM (SELECT DISTINCT ON (equipe_id) equipe_id, funcao, valor FROM evento_equipe ORDER BY equipe_id, id DESC) x
WHERE x.equipe_id = e.id;
