#!/usr/bin/env python3
"""
Converte os dados do protótipo Anne Gestão (artefato) para a carga inicial do backend Java.

Entrada: o backup .json que o protótipo exporta ({"app", "exportadoEm", "colecoes": {coleção: {id: doc}}})
         ou uma pasta com um .json por documento (<pasta>/<coleção>/<id>.json).
Saída (na pasta indicada):
  - anne-gestao-carga.json       arquivo de backup do Java (BackupArquivoDTO), para POST /backup/restaurar
  - 03-evento-etapas.sql         etapas marcadas à mão (tabela evento_etapas, que o backup não cobre)
  - 04-evento-projeto.sql        logística e orçamento do evento (tabelas evento_logistica e evento_orcamento, fora do backup)
  - 05-equipe-producao.sql       escala de equipe e mapa/plano de produção (equipe, evento_equipe, evento_producao, evento_ordem_plano)
  - pendente-sem-tabela.json     dados do protótipo que ainda não têm tabela/coluna no banco
  - mapa-ids.json                id do protótipo -> id numérico, por coleção
  - relatorio.txt                contagens, ajustes feitos e avisos

Uso: python3 -I converter.py <entrada> <pasta-saida>
"""
import collections
import json
import os
import sys

APP = "Anne Gestão"
IMPORTACAO = "Importação do protótipo"

# Plano de contas semeado pela V2 (ids 1..23). O que tiver o mesmo nome no protótipo reaproveita o id.
SEED_CATEGORIAS = [
    "Eventos sociais", "Eventos corporativos", "Serviços extras", "Impostos sobre vendas", "Descontos e cancelamentos",
    "Alimentos e bebidas (A&B)", "Mão de obra de evento", "Descartáveis, embalagens e gás", "Locação de louças e mobiliário",
    "Transporte e frete", "Comissões", "Taxas de cartão e antecipação", "Degustações", "Aluguel da cozinha", "Água, luz e internet",
    "Salários fixos + encargos", "Pró-labore", "Contador e sistemas", "Marketing", "Manutenção, seguros e alvarás", "Depreciação",
    "Despesas financeiras", "IRPJ / CSLL",
]
GRUPOS = ["receita", "deducao", "custo_direto", "despesa_variavel", "despesa_fixa", "depreciacao_financeiro_ir", "investimento"]

# Duplicatas de nome no cadastro de insumos do protótipo (o banco exige nome único).
INSUMO_DESCARTAR = {"ru5k7h7k9ht8ap7sl3z6"}  # PAPEL HIGIÊNICO em kg, custo 0, sem nenhum uso (o outro, em un, fica)
INSUMO_FUNDIR = {"l8j3mkchmharwl54of5k": "smduas6eodv9ijc02j7v"}  # Batata baroa cadastrada duas vezes pela leitura de NF
INSUMO_RENOMEAR = {"ins-farinha-de-trigo-00": "Farinha de trigo 00"}  # mesmo nome de "Farinha de trigo"; é a tipo 00 (R$ 28,50/kg)

UNIDADES_INSUMO = {"kg", "l", "un", "dz", "maço", "garrafa", "botijão"}
CATEGORIAS_INSUMO = {"Proteínas e frutos do mar", "Queijos e laticínios", "Hortifruti", "Mercearia", "Bebidas", "Descartáveis",
                     "Produtos de limpeza", "Outros"}
UNIDADES_NEGOCIO = {"buffet", "restaurante", "chocolates"}
CATEGORIAS_PREP = {"canape", "volante", "entrada", "ilha", "principal", "acompanhamento", "sobremesa", "bebida", "base"}

avisos = []
ajustes = []


def carregar(entrada):
    if os.path.isdir(entrada):
        cols = {}
        for col in sorted(os.listdir(entrada)):
            d = os.path.join(entrada, col)
            if not os.path.isdir(d):
                continue
            cols[col] = {f[:-5]: json.load(open(os.path.join(d, f), encoding="utf-8"))
                         for f in sorted(os.listdir(d)) if f.endswith(".json") and not f.startswith(".")}
        return cols
    arq = json.load(open(entrada, encoding="utf-8"))
    if arq.get("app") not in (None, "", APP):
        sys.exit("Este arquivo não é um backup do Anne Gestão.")
    return arq["colecoes"]


def vazio(v):
    return v is None or v == ""


def texto(v):
    return "" if v is None else str(v)


def checar_tamanho(onde, valor, maximo):
    if valor is not None and len(str(valor)) > maximo:
        avisos.append(f"{onde}: {len(str(valor))} caracteres (máx. {maximo})")


def tipo_item(i):
    if i.get("tipo"):
        return i["tipo"]
    return {"Descartáveis": "embalagem", "Produtos de limpeza": "limpeza"}.get(i.get("categoria"), "ingrediente")


def local_item(i):
    if i.get("local"):
        return i["local"]
    por_categoria = {"Proteínas e frutos do mar": "Refrigerado", "Queijos e laticínios": "Refrigerado", "Hortifruti": "Refrigerado",
                     "Mercearia": "Seco", "Bebidas": "Seco", "Produtos de limpeza": "Depósito limpeza"}
    if i.get("categoria") in por_categoria:
        return por_categoria[i["categoria"]]
    t = i.get("tipo")
    return "Depósito limpeza" if t == "limpeza" else "Depósito de louças" if t in ("louca", "utensilio") else "Depósito"


def praca_padrao(categoria):
    return {"canape": "Frios", "ilha": "Frios", "bebida": "Frios", "volante": "Salgados", "sobremesa": "Confeitaria"}.get(categoria, "Cozinha quente")


def numerar(ids):
    return {k: n for n, k in enumerate(ids, start=1)}


def main(entrada, saida):
    C = carregar(entrada)
    get = lambda c: C.get(c) or {}
    cats, clis, ins, preps, evs = get("categorias"), get("clientes"), get("insumos"), get("preparacoes"), get("eventos")
    card, parcs, extras, nfs, lancs = get("cardapio"), get("parcelas"), get("extras"), get("nfs"), get("lancamentos")
    movs, execs = get("movimentos"), get("cozinha_exec")
    for vaziaEsperada in ("extras", "pratos_venda", "lotes", "producoes"):
        if get(vaziaEsperada):
            avisos.append(f"A coleção {vaziaEsperada} tem {len(get(vaziaEsperada))} registros e este conversor não a trata.")

    # ------------------------------------------------------------------ ids
    seed = {nome: n for n, nome in enumerate(SEED_CATEGORIAS, start=1)}
    m_cat, novas = {}, []
    for k, c in cats.items():
        if c["nome"] in seed:
            m_cat[k] = seed[c["nome"]]
        else:
            novas.append(k)
    novas.sort(key=lambda k: (GRUPOS.index(cats[k]["grupo"]) if cats[k]["grupo"] in GRUPOS else 99, cats[k].get("ordem", 0), cats[k]["nome"]))
    for n, k in enumerate(novas, start=len(SEED_CATEGORIAS) + 1):
        m_cat[k] = n

    m_cli = numerar(sorted(clis, key=lambda k: (clis[k].get("criadoEm", ""), k)))

    # Insumos: descarta, funde e renomeia duplicatas antes de numerar.
    usados = collections.Counter()
    for p in preps.values():
        for it in (p.get("itens") or []) + (p.get("itensProducaoAnterior") or []):
            usados[it.get("insumoId")] += 1
    for n in nfs.values():
        for it in n.get("itens") or []:
            usados[it.get("insumoId")] += 1
    for m in movs.values():
        usados[m.get("insumoId")] += 1
    for x in execs.values():
        for k in (x.get("itens") or {}):
            usados[k] += 1
    for k in INSUMO_DESCARTAR:
        if k in ins:
            if usados[k]:
                sys.exit(f"Insumo {k} marcado para descarte está em uso ({usados[k]} referências).")
            ajustes.append(f"Insumo descartado (nome repetido, sem uso): {ins[k]['nome']} [{k}, {ins[k]['unidade']}]")
    alias = {}
    for de, para in INSUMO_FUNDIR.items():
        if de in ins and para in ins:
            alias[de] = para
            a, b = ins[de], ins[para]
            if a.get("atualizadoEm", "") > b.get("atualizadoEm", ""):
                for campo in ("custo", "fornecedor", "atualizadoEm"):
                    b[campo] = a.get(campo)
            ajustes.append(f"Insumo fundido (nome repetido): {a['nome']} [{de}] -> [{para}], {usados[de]} referências movidas; custo final R$ {b['custo']}")
    for k, nome in INSUMO_RENOMEAR.items():
        if k in ins:
            ajustes.append(f"Insumo renomeado (nome repetido): {ins[k]['nome']} [{k}] -> {nome}")
            ins[k]["nome"] = nome
    ins_ok = {k: v for k, v in ins.items() if k not in INSUMO_DESCARTAR and k not in alias}
    m_ins = numerar(sorted(ins_ok, key=lambda k: (ins_ok[k]["nome"].strip().lower(), k)))
    for de, para in alias.items():
        m_ins[de] = m_ins[para]
    repetidos = [n for n, q in collections.Counter(v["nome"] for v in ins_ok.values()).items() if q > 1]
    if repetidos:
        sys.exit(f"Insumos com nome repetido: {repetidos}")

    # Fichas: sub-receitas primeiro, para existirem quando a ficha que as usa for gravada.
    subs = {it["subId"] for p in preps.values() for it in p.get("itens") or [] if it.get("subId")}
    m_prep = numerar(sorted(preps, key=lambda k: (k not in subs, k)))
    m_ev = numerar(sorted(evs, key=lambda k: (evs[k]["codigo"], k)))
    m_card = numerar(sorted(card, key=lambda k: (m_ev[card[k]["eventoId"]], k)))
    m_parc = numerar(sorted(parcs, key=lambda k: (m_ev[parcs[k]["eventoId"]], parcs[k]["numero"], k)))
    m_nf = numerar(sorted(nfs, key=lambda k: (nfs[k]["emissao"], nfs[k].get("criadoEm", ""), k)))
    m_lanc = numerar(sorted(lancs, key=lambda k: (lancs[k].get("criadoEm", ""), k)))
    m_mov = numerar(sorted(movs, key=lambda k: (movs[k].get("criadoEm", ""), k)))

    def ref(mapa, k, onde):
        if vazio(k):
            return None
        if k not in mapa:
            raise KeyError(f"{onde}: referência a {k!r} que não existe")
        return mapa[k]

    # ------------------------------------------------------------------ documentos (formato dos *ResponseDTO)
    out = collections.OrderedDict()

    out["categorias"] = {str(m_cat[k]): {"nome": c["nome"], "grupo": c["grupo"], "ordem": int(c.get("ordem") or 0)}
                         for k, c in sorted(cats.items(), key=lambda kv: m_cat[kv[0]])}

    out["clientes"] = {}
    for k in sorted(clis, key=m_cli.get):
        c = clis[k]
        out["clientes"][str(m_cli[k])] = {"nome": c["nome"], "tipo": c.get("tipo") or "PF", "documento": texto(c.get("documento")),
                                          "telefone": texto(c.get("telefone")), "email": texto(c.get("email")), "origem": texto(c.get("origem")),
                                          "criadoEm": c["criadoEm"]}
        for campo, mx in (("nome", 200), ("documento", 30), ("telefone", 40), ("email", 120), ("origem", 20)):
            checar_tamanho(f"cliente {k}.{campo}", c.get(campo), mx)

    out["insumos"] = {}
    for k in sorted(ins_ok, key=m_ins.get):
        i = ins_ok[k]
        if i["unidade"] not in UNIDADES_INSUMO:
            avisos.append(f"insumo {k}: unidade {i['unidade']!r} fora da lista do Java")
        if i["categoria"] not in CATEGORIAS_INSUMO:
            avisos.append(f"insumo {k}: categoria {i['categoria']!r} fora da lista do Java")
        out["insumos"][str(m_ins[k])] = {"nome": i["nome"].strip(), "tipo": tipo_item(i), "categoria": i["categoria"], "unidade": i["unidade"],
                                         "local": local_item(i), "unidadeCompra": texto(i.get("unidadeCompra")), "fatorCompra": i.get("fatorCompra"),
                                         "estoqueMinimo": i.get("estoqueMinimo") or 0, "custo": i.get("custo") or 0,
                                         "fornecedor": texto(i.get("fornecedor")), "atualizadoEm": i["atualizadoEm"][:10]}
        for campo, mx in (("nome", 255), ("unidadeCompra", 60), ("fornecedor", 255)):
            checar_tamanho(f"insumo {k}.{campo}", i.get(campo), mx)

    out["preparacoes"] = {}
    for k in sorted(preps, key=m_prep.get):
        p = preps[k]
        if p["categoria"] not in CATEGORIAS_PREP:
            avisos.append(f"ficha {k}: categoria {p['categoria']!r} fora da lista do Java")
        if not p.get("praca"):
            ajustes.append(f"Ficha sem praça recebeu a praça padrão da categoria: {p['nome']} -> {praca_padrao(p['categoria'])}")
        itens = []
        for it in p.get("itens") or []:
            ins_id = ref(m_ins, alias.get(it.get("insumoId"), it.get("insumoId")), f"ficha {k}")
            sub_id = ref(m_prep, it.get("subId"), f"ficha {k}")
            if (ins_id is None) == (sub_id is None):
                raise ValueError(f"ficha {k}: item sem insumo nem sub-receita (ou com os dois): {it}")
            itens.append({"insumoId": ins_id, "subId": sub_id, "qtd": it["qtd"], "fc": it.get("fc") or 1})
        out["preparacoes"][str(m_prep[k])] = {
            "nome": p["nome"], "categoria": p["categoria"], "praca": p.get("praca") or praca_padrao(p["categoria"]),
            "conservacao": p.get("conservacao") or "refrigerado", "validadeDias": p.get("validadeDias"),
            "setor": texto(p.get("setor")), "dnProducao": p.get("dnProducao"), "tempoMin": p.get("tempoMin"),
            "equipamentos": texto(p.get("equipamentos")), "armazenamento": texto(p.get("armazenamento")),
            "compradoPronto": bool(p.get("compradoPronto")), "rendimento": p["rendimento"],
            "unidadeRendimento": p.get("unidadeRendimento") or "porções", "porcaoPadrao": p.get("porcaoPadrao") or 1, "itens": itens,
            "loucas": [{"insumoId": ref(m_ins, alias.get(l["insumoId"], l["insumoId"]), f"ficha {k} louça"), "qtd": l["qtd"]} for l in p.get("loucas") or []],
            "etapas": [{"desc": texto(e.get("desc")), "praca": texto(e.get("praca")), "dn": e.get("dn")} for e in p.get("etapas") or []],
            "componentes": [{"n": c.get("n"), "u": c.get("u"), "q": c.get("q"), "cons": c.get("cons"), "obs": c.get("obs")} for c in p.get("componentes") or []],
            "obsEnvio": texto(p.get("obsEnvio")), "modoPreparo": texto(p.get("modoPreparo")), "atualizadoEm": p["atualizadoEm"][:10]}
        for campo, mx in (("nome", 255), ("praca", 20), ("obsEnvio", 300), ("modoPreparo", 4000)):
            checar_tamanho(f"ficha {k}.{campo}", p.get(campo), mx)

    out["eventos"] = {}
    for k in sorted(evs, key=m_ev.get):
        e = evs[k]
        ct = e.get("contrato")
        contrato = None if not ct else {
            "numero": texto(ct.get("numero")), "dataAssinatura": texto(ct.get("dataAssinatura")),
            "convidadosContratados": int(ct.get("convidadosContratados") or 0), "valorPorConvidado": ct.get("valorPorConvidado") or 0,
            "valorTotal": ct.get("valorTotal") or 0, "valorConvidadoExtra": ct.get("valorConvidadoExtra") or 0,
            "categoriaId": ref(m_cat, ct.get("categoriaId"), f"evento {k} contrato")}
        plano = e.get("planoCozinha")
        if "margem" not in e:
            ajustes.append(f"Evento sem margem recebeu 0%: {e['codigo']} {e['nome']}")
        out["eventos"][str(m_ev[k])] = {
            "codigo": e["codigo"], "nome": e["nome"], "clienteId": ref(m_cli, e["clienteId"], f"evento {k}"), "tipo": e.get("tipo") or "outro",
            "status": e.get("status") or "lead", "dataEvento": texto(e.get("dataEvento")), "horaInicio": texto(e.get("horaInicio")),
            "local": texto(e.get("local")), "cidade": texto(e.get("cidade")), "convidadosPrevistos": e.get("convidadosPrevistos"),
            "margem": e.get("margem") or 0, "observacoes": texto(e.get("observacoes")), "contrato": contrato,
            "cardapioFechadoEm": texto(e.get("cardapioFechadoEm")), "envioExtras": e.get("envioExtras") or [],
            "planoCozinha": None if not plano else [{"k": t.get("k"), "desc": t.get("desc"), "praca": t.get("praca"), "dn": t.get("dn"),
                                                     "produtos": t.get("produtos"), "resp": t.get("resp")} for t in plano],
            "equipeCozinha": texto(e.get("equipeCozinha")), "criadoEm": e["criadoEm"]}
        for campo, mx in (("codigo", 6), ("nome", 255), ("local", 200), ("cidade", 100), ("observacoes", 4000), ("equipeCozinha", 200)):
            checar_tamanho(f"evento {k}.{campo}", e.get(campo), mx)

    out["cardapio"] = {}
    for k in sorted(card, key=m_card.get):
        c = card[k]
        out["cardapio"][str(m_card[k])] = {"eventoId": ref(m_ev, c["eventoId"], f"cardápio {k}"), "preparacaoId": ref(m_prep, c["preparacaoId"], f"cardápio {k}"),
                                           "porcao": c["porcao"], "status": c.get("status") or "sim", "praca": c.get("praca") or None,
                                           "responsavel": texto(c.get("responsavel"))}

    # Pagamentos: lançamento realizado vira um pagamento único na data do pagamento (como a V4 fez com os antigos).
    lanc_da_parcela = {l["parcelaId"]: k for k, l in lancs.items() if l.get("parcelaId")}

    def pagamentos(l):
        if l["status"] != "realizado":
            return []
        forma = l.get("formaPag") or (parcs.get(l.get("parcelaId")) or {}).get("forma") or ""
        return [{"id": None, "data": l.get("dataPagamento") or l["dataCompetencia"], "valor": l["valor"], "conta": l.get("conta") or "banco",
                 "forma": forma, "em": l["criadoEm"], "por": IMPORTACAO}]

    out["parcelas"] = {}
    for k in sorted(parcs, key=m_parc.get):
        p = parcs[k]
        lk = lanc_da_parcela.get(k)
        out["parcelas"][str(m_parc[k])] = {"eventoId": ref(m_ev, p["eventoId"], f"parcela {k}"), "numero": p["numero"], "vencimento": p["vencimento"],
                                           "valor": p["valor"], "pagoEm": texto(p.get("pagoEm")), "forma": texto(p.get("forma")),
                                           "lancamentoId": m_lanc.get(lk), "recebido": sum(x["valor"] for x in pagamentos(lancs[lk])) if lk else 0}

    out["extras"] = {}

    out["nfs"] = {}
    for k in sorted(nfs, key=m_nf.get):
        n = nfs[k]
        itens = [{"desc": texto(i.get("desc")), "cod": texto(i.get("cod")), "uCom": texto(i.get("uCom")),
                  "insumoId": ref(m_ins, alias.get(i.get("insumoId"), i.get("insumoId")), f"NF {k}"), "qtdNota": i.get("qtdNota") or 0,
                  "unid": i.get("unid") or "estoque", "qtd": i.get("qtd") or 0, "valorUnit": i.get("valorUnit"), "valor": i.get("valor") or 0,
                  "conferido": bool(i.get("conferido"))} for i in n.get("itens") or []]
        out["nfs"][str(m_nf[k])] = {
            "fornecedor": n["fornecedor"], "numero": texto(n.get("numero")), "emissao": n["emissao"], "eventoId": ref(m_ev, n.get("eventoId"), f"NF {k}"),
            "lancarCaixa": bool(n.get("lancarCaixa", True)), "categoriaId": ref(m_cat, n.get("categoriaId"), f"NF {k}"), "formaPag": n.get("formaPag") or "boleto",
            "parcelas": [{"venc": p["venc"], "valor": p["valor"], "pago": bool(p.get("pago"))} for p in n.get("parcelas") or []],
            "pago": bool(n.get("pago")), "status": n.get("status") or "rascunho", "itens": itens, "acrescimo": n.get("acrescimo") or 0,
            "total": n.get("total") or 0, "chave": texto(n.get("chave")), "serie": texto(n.get("serie")), "razaoSocial": texto(n.get("razaoSocial")),
            "cnpjFornecedor": texto(n.get("cnpjFornecedor")), "importadaXML": bool(n.get("importadaXML")), "importadaFoto": bool(n.get("importadaFoto")),
            "movIds": [m_mov[m] for m in n.get("movIds") or [] if m in m_mov], "lancIds": [m_lanc[x] for x in n.get("lancIds") or [] if x in m_lanc],
            "lancadaEm": texto(n.get("lancadaEm")), "criadoEm": n["criadoEm"], "unidade": n.get("unidade") or None}
        for campo, mx in (("fornecedor", 120), ("numero", 20), ("serie", 10), ("chave", 44), ("razaoSocial", 200), ("cnpjFornecedor", 18)):
            checar_tamanho(f"NF {k}.{campo}", n.get(campo), mx)
        for i in n.get("itens") or []:
            checar_tamanho(f"NF {k} item.desc", i.get("desc"), 200)
            checar_tamanho(f"NF {k} item.uCom", i.get("uCom"), 10)

    out["lancamentos"] = {}
    for k in sorted(lancs, key=m_lanc.get):
        l = lancs[k]
        out["lancamentos"][str(m_lanc[k])] = {
            "tipo": l["tipo"], "categoriaId": ref(m_cat, l["categoriaId"], f"lançamento {k}"), "eventoId": ref(m_ev, l.get("eventoId"), f"lançamento {k}"),
            "descricao": l["descricao"], "valor": l["valor"], "dataCompetencia": l["dataCompetencia"], "dataPagamento": texto(l.get("dataPagamento")),
            "vencimento": texto(l.get("vencimento")), "status": l["status"], "origem": l.get("origem") or "manual", "conta": l.get("conta") or "banco",
            "parcelaId": ref(m_parc, l.get("parcelaId"), f"lançamento {k}"), "extraId": None, "nfId": ref(m_nf, l.get("nfId"), f"lançamento {k}"),
            "parcelaNF": l.get("parcelaNF"), "formaPag": l.get("formaPag"), "fornecedor": l.get("fornecedor"), "criadoEm": l["criadoEm"],
            "unidade": l.get("unidade") or None, "unidadeOrigem": None, "unidadeDestino": None, "insumoId": None, "item": None, "qtd": None,
            "unid": None, "responsavel": None, "obs": None, "pagamentos": pagamentos(l),
            "criadoPor": IMPORTACAO if l.get("criadoPor") else None, "alteradoPor": IMPORTACAO if l.get("alteradoPor") else None,
            "alteradoEm": l.get("alteradoEm") or None}
        for campo, mx in (("descricao", 255), ("fornecedor", 120), ("formaPag", 15)):
            checar_tamanho(f"lançamento {k}.{campo}", l.get(campo), mx)

    out["movimentos"] = {}
    for k in sorted(movs, key=m_mov.get):
        m = movs[k]
        ins_k = alias.get(m["insumoId"], m["insumoId"])
        unidade_estoque = ins_ok[ins_k]["unidade"]
        tipo, origem, motivo = m["tipo"], m.get("origem") or "manual", texto(m.get("motivo"))
        unidade_uso = None
        if tipo == "consumo":
            # V9: consumo é tipo próprio; no protótipo a unidade de negócio que usou vem no campo "unidade".
            unidade_uso = m.get("unidadeUso") or (m.get("unidade") if m.get("unidade") in UNIDADES_NEGOCIO else None)
            if not unidade_uso:
                sys.exit(f"movimento {k}: consumo sem unidade de negócio")
        out["movimentos"][str(m_mov[k])] = {
            "tipo": tipo, "insumoId": m_ins[ins_k], "qtd": m["qtd"], "qtdInformada": m.get("qtdInformada") or 0, "unidade": unidade_estoque,
            "data": m["data"], "motivo": motivo, "eventoId": ref(m_ev, m.get("eventoId"), f"movimento {k}"), "custoUnit": m.get("custoUnit"),
            "origem": origem, "nfId": ref(m_nf, m.get("nfId"), f"movimento {k}"), "criadoEm": m["criadoEm"],
            "validade": texto(m.get("validade")), "unidadeUso": unidade_uso}
        checar_tamanho(f"movimento {k}.motivo", motivo, 300)

    # Execução da cozinha: tarefas (feito/responsável/dia) e compras marcadas. As chaves embutem ids do protótipo.
    out["cozinha_exec"] = {}
    vistos = []
    for k in sorted(execs, key=lambda k: (m_ev.get(execs[k]["eventoId"], 0), k)):
        x = execs[k]
        ev = ref(m_ev, x["eventoId"], f"execução {k}")
        key, itens, card_id = x.get("key"), {}, None
        if x["kind"] == "comp":
            # Componente separado pelos insumos da ficha: key = "f_<insumo>_<sub-receita>" (Java: Cozinha.prato).
            if x.get("cardapioId") not in m_card:
                avisos.append(f"execução {k}: item de cardápio {x.get('cardapioId')} não existe mais; registro ignorado")
                continue
            card_id = m_card[x["cardapioId"]]
            if key.startswith("f_"):
                ik, _, via = key[2:].partition("_")
                key = f"f_{ref(m_ins, alias.get(ik, ik), f'execução {k}')}_{via}"
            chave = f"c_{card_id}__{key}"
        elif x["kind"] == "tarefa":
            pre, _, resto = key.partition("_")
            if pre == "p":
                if resto not in m_card:
                    avisos.append(f"execução {k}: item de cardápio {resto} não existe mais; registro ignorado")
                    continue
                key = f"p_{m_card[resto]}"
            elif pre == "e":
                cid, _, ix = resto.rpartition("_")
                if cid not in m_card:
                    avisos.append(f"execução {k}: item de cardápio {cid} não existe mais; registro ignorado")
                    continue
                key = f"e_{m_card[cid]}_{ix}"
            chave = f"t_{ev}__{key}"
        elif x["kind"] == "compras":
            for ik, marcado in (x.get("itens") or {}).items():
                nid = str(ref(m_ins, alias.get(ik, ik), f"execução {k}"))
                itens[nid] = bool(itens.get(nid)) or bool(marcado)
            key, chave = None, f"cp_{ev}"
        else:
            avisos.append(f"execução {k}: tipo {x['kind']!r} não tratado; registro ignorado")
            continue
        vistos.append(chave)
        out["cozinha_exec"][str(len(out["cozinha_exec"]) + 1)] = {
            "chave": chave, "kind": x["kind"], "eventoId": ev, "cardapioId": card_id, "insumoId": None, "key": key,
            "produzido": x.get("produzido"), "produzidoEm": texto(x.get("produzidoEm")), "executadoPor": texto(x.get("executadoPor")),
            "enviadoQ": x.get("enviadoQ"), "enviado": bool(x.get("enviado")), "enviadoEm": texto(x.get("enviadoEm")),
            "separadoPor": texto(x.get("separadoPor")), "retorno": x.get("retorno"), "conferidoPor": texto(x.get("conferidoPor")),
            "obs": texto(x.get("obs")), "tempSaida": x.get("tempSaida"), "tempChegada": x.get("tempChegada"), "saiu": x.get("saiu"),
            "voltou": x.get("voltou"), "avaria": x.get("avaria"),
            "feito": bool(x.get("feito")), "resp": texto(x.get("resp")), "dnSet": bool(x.get("dnSet")), "dn": x.get("dn"), "itens": itens,
            "chefPor": "", "chefHora": "", "cargaPor": "", "cargaHora": "", "recebPor": "", "recebHora": "", "retornoPor": "", "retornoHora": "",
            "veiculo": "", "atualizadoEm": x["atualizadoEm"]}
    if len(vistos) != len(set(vistos)):
        sys.exit("Execuções da cozinha com chave repetida.")

    for col in ("pratos_venda", "lotes"):
        out[col] = {}

    # ------------------------------------------------------------------ conferências de unicidade do banco
    def unico(nome, valores):
        rep = [v for v, q in collections.Counter(valores).items() if q > 1]
        if rep:
            sys.exit(f"Valores repetidos em {nome}: {rep[:5]}")

    unico("producao.codigo", [e["codigo"] for e in out["eventos"].values()])
    unico("parcelas (evento, número)", [(p["eventoId"], p["numero"]) for p in out["parcelas"].values()])
    unico("lancamentos.parcela_id", [l["parcelaId"] for l in out["lancamentos"].values() if l["parcelaId"]])
    unico("notas_fiscais.chave", [n["chave"] for n in out["nfs"].values() if n["chave"]])

    arquivo = {"app": APP, "exportadoEm": max(l["criadoEm"] for l in lancs.values()) if lancs else "", "colecoes": out}

    # ------------------------------------------------------------------ etapas do evento (tabela evento_etapas)
    sql = ["-- Etapas do evento marcadas à mão no protótipo. Rodar DEPOIS da restauração do anne-gestao-carga.json.",
           "-- O evento é localizado pelo código; uma etapa já gravada não é sobrescrita.", "BEGIN;"]
    n_etapas = 0
    for k in sorted(evs, key=m_ev.get):
        for etapa, st in sorted((evs[k].get("etapas") or {}).items()):
            if st.get("feito") or st.get("dispensada"):
                n_etapas += 1
                sql.append("INSERT INTO evento_etapas (evento_id, etapa, feito, dispensada, atualizado_em, atualizado_por) "
                           f"SELECT id, '{etapa}', {str(bool(st.get('feito'))).upper()}, {str(bool(st.get('dispensada'))).upper()}, "
                           f"'{st.get('em') or ''}'::timestamptz, '{IMPORTACAO}' FROM producao WHERE codigo = '{evs[k]['codigo']}' "
                           "ON CONFLICT (evento_id, etapa) DO NOTHING;")
    sql.append("COMMIT;")

    # ------------------------------------------------------------------ evento como projeto (V11: evento_logistica, evento_orcamento)
    def lit(v):
        return "'" + texto(v).replace("'", "''") + "'"

    def hora(onde, v):
        v = texto(v).strip()
        if not v:
            return "NULL"
        if len(v) != 5 or v[2] != ":" or not (v[:2].isdigit() and v[3:].isdigit()) or int(v[:2]) > 23 or int(v[3:]) > 59:
            sys.exit(f"{onde}: horário inválido {v!r} (use HH:MM)")
        return f"'{v}'::time"

    def dinheiro(onde, v):
        if v is None or v == "":
            return None
        n = round(float(v), 2)
        if n < 0:
            sys.exit(f"{onde}: valor negativo {v!r}")
        return f"{n:.2f}"

    sql_proj = ["-- Logística e orçamento do evento salvos no protótipo (V11). Rodar DEPOIS da restauração do anne-gestao-carga.json.",
                "-- O evento é localizado pelo código; o que já estiver gravado no banco não é sobrescrito.", "BEGIN;"]
    n_log = n_orc = 0
    for k in sorted(evs, key=m_ev.get):
        e = evs[k]
        cod = e["codigo"]
        L = e.get("logistica")
        if L:
            for campo, mx in (("veiculos", 200), ("motorista", 120), ("viagens", 120), ("obs", 2000)):
                checar_tamanho(f"evento {cod} logística {campo}", texto(L.get(campo)).strip(), mx)
            n_log += 1
            sql_proj.append("INSERT INTO evento_logistica (evento_id, veiculos, motorista, viagens, saida, montagem, retorno, obs, atualizado_em, atualizado_por) "
                            f"SELECT id, {lit(texto(L.get('veiculos')).strip())}, {lit(texto(L.get('motorista')).strip())}, "
                            f"{lit(texto(L.get('viagens')).strip())}, {hora(f'evento {cod} saída', L.get('saida'))}, "
                            f"{hora(f'evento {cod} montagem', L.get('montagem'))}, {hora(f'evento {cod} retorno', L.get('retorno'))}, "
                            f"{lit(texto(L.get('obs')).strip())}, {lit(L.get('atualizadoEm') or '')}::timestamptz, '{IMPORTACAO}' "
                            f"FROM producao WHERE codigo = '{cod}' ON CONFLICT (evento_id) DO NOTHING;")
        O = e.get("orcamento")
        if O:
            n_orc += 1
            equipe = dinheiro(f"evento {cod} orçamento equipe", O.get("equipe"))
            sql_proj.append("INSERT INTO evento_orcamento (evento_id, equipe, transporte, outros, atualizado_em, atualizado_por) "
                            f"SELECT id, {equipe or 'NULL'}, {dinheiro(f'evento {cod} transporte', O.get('transporte')) or '0'}, "
                            f"{dinheiro(f'evento {cod} outros', O.get('outros')) or '0'}, {lit(O.get('atualizadoEm') or '')}::timestamptz, "
                            f"'{IMPORTACAO}' FROM producao WHERE codigo = '{cod}' ON CONFLICT (evento_id) DO NOTHING;")
        if e.get("fechamento"):
            avisos.append(f"evento {cod}: retrato de fechamento do protótipo não é importado (feche de novo pelo sistema)")
    sql_proj.append("COMMIT;")

    # ------------------------------------------------------------------ o que ainda não tem tabela
    cz = get("cozinha")
    pend = collections.OrderedDict()
    pend["_leia"] = ("Dados do protótipo sem tabela/coluna no banco atual, já com os ids numéricos da carga. "
                     "Ficam guardados para a migração que criar esses campos.")
    pend["clientes.observacoes"] = {str(m_cli[k]): c["observacoes"] for k, c in clis.items() if c.get("observacoes")}
    pend["eventos.producaoPara"] = {str(m_ev[k]): e["producaoPara"] for k, e in evs.items() if e.get("producaoPara") is not None}
    def chave_ordem(ok):
        if ok.startswith("m_") and ok[2:] in m_card:
            return f"m_{m_card[ok[2:]]}"
        if ok.startswith("s_") and ok[2:] in m_prep:
            return f"s_{m_prep[ok[2:]]}"
        if ok.startswith("f_") and ok[2:] in m_card:
            return f"f_{m_card[ok[2:]]}"
        return None

    # ------------------------------------------------------------------ escala e mapa de produção (V6/V7 e V8, fora do backup)
    import unicodedata

    def norm(s):
        return " ".join(unicodedata.normalize("NFD", texto(s)).encode("ascii", "ignore").decode().lower().split())

    SETORES = {"quente", "fria", "confeitaria", "montagem", "expedicao"}
    sql_eq = ["-- Escala de equipe (V6/V7) e mapa de produção do evento (V8) salvos no protótipo. Rodar DEPOIS da restauração",
              "-- do anne-gestao-carga.json. O evento é localizado pelo código; o que já estiver gravado não é sobrescrito.", "BEGIN;"]
    pessoas = collections.OrderedDict()
    for k in sorted(evs, key=m_ev.get):
        for p in evs[k].get("equipe") or []:
            pessoas.setdefault(norm(p["nome"]), p)
    for p in pessoas.values():
        checar_tamanho(f"equipe {p['nome']}", p["nome"], 120)
        sql_eq.append("INSERT INTO equipe (nome, contato, pix, funcao_padrao, diaria_padrao, criado_por) "
                      f"SELECT {lit(p['nome'].strip())}, {lit(texto(p.get('contato')).strip())}, {lit(texto(p.get('pix')).strip())}, "
                      f"{lit(p.get('funcao') or 'Garçom')}, {dinheiro('equipe ' + p['nome'], p.get('valor')) or '0'}, '{IMPORTACAO}' "
                      f"WHERE NOT EXISTS (SELECT 1 FROM equipe WHERE lower(nome) = lower({lit(p['nome'].strip())}));")
    n_escala = 0
    for k in sorted(evs, key=m_ev.get):
        cod = evs[k]["codigo"]
        for p in evs[k].get("equipe") or []:
            n_escala += 1
            sql_eq.append("INSERT INTO evento_equipe (evento_id, equipe_id, funcao, chegada, saida, valor, criado_por) "
                          f"SELECT e.id, q.id, {lit(p.get('funcao') or 'Garçom')}, {hora(f'evento {cod} chegada', p.get('chegada'))}, "
                          f"{hora(f'evento {cod} saída', p.get('saida'))}, {dinheiro(f'evento {cod} diária', p.get('valor')) or '0'}, '{IMPORTACAO}' "
                          f"FROM producao e, equipe q WHERE e.codigo = '{cod}' AND lower(q.nome) = lower({lit(p['nome'].strip())}) "
                          "ON CONFLICT (evento_id, equipe_id) DO NOTHING;")
    n_mapa = n_plano = 0
    for k in sorted(evs, key=m_ev.get):
        e, cod = evs[k], evs[k]["codigo"]
        P = e.get("producao")
        if not P:
            continue
        n_mapa += 1
        sql_eq.append("INSERT INTO evento_producao (evento_id, gerado_em, gerado_por, liberado_em, responsavel, horarios) "
                      f"SELECT id, {lit(P.get('geradoEm') or '')}::timestamptz, '{IMPORTACAO}', "
                      f"{(lit(P['liberadoEm']) + '::timestamptz') if P.get('liberadoEm') else 'NULL'}, {lit(texto(P.get('responsavel')).strip())}, "
                      f"{lit(texto(P.get('horarios')).strip())} FROM producao WHERE codigo = '{cod}' ON CONFLICT (evento_id) DO NOTHING;")
        for ok, v in sorted((P.get("plano") or {}).items()):
            chave = chave_ordem(ok)
            if chave is None:
                avisos.append(f"evento {cod}: plano da ordem {ok} aponta para item que não existe mais; ignorado")
                continue
            setor = v.get("setor") or "quente"
            if setor not in SETORES:
                sys.exit(f"evento {cod}: setor {setor!r} da ordem {ok} não existe em setores_producao")
            n_plano += 1
            sql_eq.append("INSERT INTO evento_ordem_plano (evento_id, chave, dn, hora, setor, responsavel, atualizado_por) "
                          f"SELECT id, '{chave}', {int(v.get('dn') or 0)}, {hora(f'evento {cod} ordem {ok}', v.get('hora'))}, '{setor}', "
                          f"{lit(texto(v.get('resp')).strip())}, '{IMPORTACAO}' FROM producao WHERE codigo = '{cod}' "
                          "ON CONFLICT (evento_id, chave) DO NOTHING;")
    sql_eq.append("COMMIT;")

    campos_prep = ("fonteIngredientes", "origemCatalogo")
    pend["preparacoes.producao"] = {}
    for k, p in preps.items():
        extra = {c: p[c] for c in campos_prep if not vazio(p.get(c))}
        if p.get("itensProducaoAnterior"):
            extra["itensProducaoAnterior"] = [{"insumoId": m_ins[alias.get(i["insumoId"], i["insumoId"])], "qtd": i["qtd"], "fc": i.get("fc") or 1}
                                              for i in p["itensProducaoAnterior"]]
        if extra:
            pend["preparacoes.producao"][str(m_prep[k])] = extra
    pend["movimentos.criadoPor"] = {str(m_mov[k]): IMPORTACAO for k, m in movs.items() if m.get("criadoPor")}
    pend["nfs.ajusteParcelas"] = {str(m_nf[k]): n["ajusteParcelas"] for k, n in nfs.items() if "ajusteParcelas" in n}

    mapa = {"categorias": m_cat, "clientes": m_cli, "insumos": m_ins, "preparacoes": m_prep, "eventos": m_ev, "cardapio": m_card,
            "parcelas": m_parc, "nfs": m_nf, "lancamentos": m_lanc, "movimentos": m_mov}

    # ------------------------------------------------------------------ gravação
    os.makedirs(saida, exist_ok=True)

    def gravar(nome, conteudo):
        with open(os.path.join(saida, nome), "w", encoding="utf-8") as f:
            if isinstance(conteudo, str):
                f.write(conteudo)
            else:
                json.dump(conteudo, f, ensure_ascii=False, indent=1)
                f.write("\n")

    gravar("anne-gestao-carga.json", arquivo)
    gravar("03-evento-etapas.sql", "\n".join(sql) + "\n")
    gravar("04-evento-projeto.sql", "\n".join(sql_proj) + "\n")
    gravar("05-equipe-producao.sql", "\n".join(sql_eq) + "\n")
    gravar("pendente-sem-tabela.json", pend)
    gravar("mapa-ids.json", mapa)

    rel = ["Carga inicial do Anne Gestão — relatório da conversão", "",
           "Registros no anne-gestao-carga.json:"]
    rel += [f"  {c:14s} {len(v):5d}" for c, v in out.items()]
    rel += [f"  {'total':14s} {sum(len(v) for v in out.values()):5d}", "",
            f"Categorias: {sum(1 for k in cats if cats[k]['nome'] in seed)} reaproveitam o id da V2, {len(novas)} novas (ids {len(SEED_CATEGORIAS) + 1}..{len(SEED_CATEGORIAS) + len(novas)}).",
            f"Etapas de evento no 03-evento-etapas.sql: {n_etapas}.",
            f"No 04-evento-projeto.sql: logística de {n_log} evento(s) e orçamento de {n_orc} evento(s).",
            f"No 05-equipe-producao.sql: {len(pessoas)} pessoa(s) no cadastro, {n_escala} escala(s), mapa de produção de {n_mapa} evento(s) e {n_plano} plano(s) de ordem.",
            "Fora da carga: coleção 'cozinha' (retrato calculado pelo protótipo; o Java recalcula) e 'sistema' (registro do último backup do protótipo).",
            "", "Ajustes feitos nos dados:"] + [f"  - {a}" for a in ajustes]
    rel += ["", "Pendente (sem tabela/coluna no banco), ver pendente-sem-tabela.json:"]
    rel += [f"  - {c}: {len(v)} registro(s)" for c, v in pend.items() if c != "_leia"]
    rel += ["", "Avisos:"] + ([f"  - {a}" for a in avisos] or ["  (nenhum)"])
    gravar("relatorio.txt", "\n".join(rel) + "\n")
    print("\n".join(rel))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
