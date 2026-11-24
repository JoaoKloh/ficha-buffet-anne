-- Logística e orçamento do evento salvos no protótipo (V11). Rodar DEPOIS da restauração do anne-gestao-carga.json.
-- O evento é localizado pelo código; o que já estiver gravado no banco não é sobrescrito.
BEGIN;
INSERT INTO evento_logistica (evento_id, veiculos, motorista, viagens, saida, montagem, retorno, obs, atualizado_em, atualizado_por) SELECT id, '2 vans', 'mauricio', '2', '09:00'::time, NULL, '23:00'::time, '', '2026-11-16T16:16:44.888Z'::timestamptz, 'Importação do protótipo' FROM producao WHERE codigo = '26-003' ON CONFLICT (evento_id) DO NOTHING;
INSERT INTO evento_orcamento (evento_id, equipe, transporte, outros, atualizado_em, atualizado_por) SELECT id, NULL, 3000.00, 0.00, '2026-11-18T00:13:59.137Z'::timestamptz, 'Importação do protótipo' FROM producao WHERE codigo = '26-003' ON CONFLICT (evento_id) DO NOTHING;
COMMIT;
