# Carga inicial do Anne Gestão (dados do protótipo)

Pasta fora do git: os arquivos têm dados pessoais de clientes.

Dados do artefato Anne Gestão (`claude.ai/artifact/VNack2goXyMPjuDupgjkq8`) lidos do banco do artefato em **24/11/2026**.

## Para produção: `carga-producao.json` + `carga-producao.sql`

| Arquivo | O que é |
|---|---|
| `carga-producao.json` | **Todos os dados, por tabela do PostgreSQL** (schema V12): `{"_meta", "tabelas": {tabela: [linhas]}}`, com os nomes reais das colunas, ids já resolvidos e as tabelas na ordem das FKs. 4.120 linhas em 21 tabelas. |
| `carga-producao.sql` | Insere o JSON acima (embutido) com `json_populate_recordset`, numa transação só. Confere antes que o banco está na V12 e sem dados de negócio, faz upsert das 23 categorias da V2 e acerta as sequências. |

Passo a passo em produção:

1. **Backup:** `pg_dump` completo.
2. **Schema na V12:** suba o backend da branch atual uma vez (o Flyway aplica V2…V12).
3. **Se já houver dados de negócio:** `psql ... -v ON_ERROR_STOP=1 -f 01-limpar-banco.sql` (mantém usuários, orçamentos e as 23 categorias da V2).
4. **Carga:** `psql ... -f carga-producao.sql`. Se qualquer conferência falhar, nada é gravado.

Teste feito em 24/11/2026: banco V12 vazio criado pelo Flyway → `carga-producao.sql` → dados idênticos, tabela por tabela, ao banco carregado pelo próprio Java; painel, cozinha, estoque, caixa e o detalhe dos 54 eventos responderam 200; um cliente novo recebeu o id seguinte (sequências corretas); rodar o SQL de novo aborta sem gravar; `01-limpar-banco.sql` + carga de novo dá o mesmo resultado.

Não entram: usuários/permissões, `flyway_schema_history`, `sistema_registros` e `setores_producao` (a V8 já semeia os mesmos 5 setores).

## Como o JSON é gerado

```
python3 -I converter.py origem-prototipo.json .          # protótipo -> backup do Java + SQLs 03/04/05
# banco descartável na V12 com o Java no ar (logado como ADMIN):
POST /backup/restaurar {"nomeArquivo": "anne-gestao-carga.json", "modo": "todos", "arquivo": <json>}   # esperado: 0 falhas
psql <descartável> -v ON_ERROR_STOP=1 -f 03-evento-etapas.sql -f 04-evento-projeto.sql -f 05-equipe-producao.sql
python3 -I gerar-carga-producao.py consulta > consulta.sql
psql <descartável> -At -f consulta.sql > dump.json
python3 -I gerar-carga-producao.py montar dump.json .
```

Assim cada linha do JSON foi gravada pelo código real da restauração do Java.

| Arquivo | O que é |
|---|---|
| `origem-prototipo.json` | Dados do artefato em 24/11/2026 (16 coleções do banco do artefato, no formato do backup do protótipo). |
| `converter.py` | Gera os arquivos abaixo. Para com erro se aparecer nome de insumo repetido, código de evento repetido ou referência quebrada. |
| `anne-gestao-carga.json` | Backup do Java (`BackupArquivoDTO`), 1.743 registros em 14 coleções (a restauração grava 1.741). |
| `03-evento-etapas.sql` | 21 etapas de evento marcadas à mão (`evento_etapas`). |
| `04-evento-projeto.sql` | Logística e orçamento do evento (`evento_logistica`, `evento_orcamento`). |
| `05-equipe-producao.sql` | Escala de equipe (`equipe`, `evento_equipe`) e mapa/plano de produção (`evento_producao`, `evento_ordem_plano`). |
| `01-limpar-banco.sql` | Apaga os dados de negócio (todas as tabelas até a V12), mantendo usuários, orçamentos e as 23 categorias da V2. |
| `pendente-sem-tabela.json` | Dados do protótipo que ainda não têm coluna no banco (ver abaixo). |
| `mapa-ids.json` | id do protótipo → id numérico, por coleção. |
| `relatorio.txt` | Contagens, ajustes feitos nos dados e avisos da última conversão. |
| `gerar-carga-producao.py` | Monta `carga-producao.json`/`.sql` a partir do banco descartável. |

## Ainda sem coluna no banco (`pendente-sem-tabela.json`)

- `clientes.observacoes` (15): endereço, RG, nomes dos noivos etc.
- `eventos.producaoPara` (1)
- `preparacoes.producao` (241): `fonteIngredientes`, `origemCatalogo` e, em 5 fichas, `itensProducaoAnterior`
- `movimentos.criadoPor` (3) e `nfs.ajusteParcelas` (2)

## Atualizar antes de produção

Se o protótipo mudar até lá, baixe as coleções de novo (ou exporte o backup .json pelo próprio protótipo), troque o `origem-prototipo.json` e refaça o fluxo acima.
