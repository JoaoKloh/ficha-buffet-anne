import type {
  CategoriaCardapio,
  ChaveInsumo,
  FuncaoEquipe,
  GrupoItens,
  ItemCardapio,
  ItemCusto,
  ItemQtdValor,
  Orcamento,
  CreateOrcamentoRequest,
} from "@/lib/models/orcamento";
import {
  SEED_BEBIDAS,
  SEED_CARDAPIO,
  SEED_DESCARTAVEIS,
  SEED_EQUIPE,
  SEED_GELO,
  SEED_LOUCAS,
  SEED_PACOTE_BEBIDAS,
} from "./orcamentoSeed";

/** Converte entrada numérica (aceita vírgula) em número finito; qualquer outra coisa vira 0. */
export function toNum(v: unknown): number {
  const n = typeof v === "string" ? parseFloat(v.replace(",", ".")) : Number(v);
  return Number.isFinite(n) ? n : 0;
}

export function novoOrcamento(): Orcamento {
  return structuredClone<Orcamento>({
    evento: {
      nome: "",
      data: "",
      local: "",
      cliente: "",
      contato: "",
      tipoServico: "Buffet completo",
      categoria: "Casamento",
      pax: 100,
      duracaoHoras: 3,
      horario: "",
    },
    verbaInsumos: { alimentos: 40.94, bebida: 3, gelo: 1.5, descartaveis: 1 },
    cardapio: SEED_CARDAPIO,
    bebidas: SEED_BEBIDAS,
    gelo: SEED_GELO,
    descartaveis: SEED_DESCARTAVEIS,
    equipe: SEED_EQUIPE,
    materiais: { frete: 350, verbaLocacao: 1972, grupos: SEED_LOUCAS },
    verbaExtra: [
      { nome: "Estacionamento visita técnica", qtd: 1, valorUnit: 0 },
      { nome: "Deslocamento visita técnica", qtd: 1, valorUnit: 0 },
      { nome: "Material gráfico", qtd: 1, valorUnit: 0 },
    ],
    degustacao: [{ nome: "Degustação para o cliente", qtd: 1, valorUnit: 0 }],
    servicosAdicionais: [],
    pacoteBebidas: { itens: SEED_PACOTE_BEBIDAS, valorVendaPorPessoa: 0 },
    margens: {
      margemBruta: 55,
      comissao: 0,
      outros: 0,
      impostos: 0,
      despesasFixas: 25,
      margemLucroBruto: 30,
      margemInvestimento: 5,
    },
    notas: "",
  });
}

// ---------- Somas genéricas ----------

export function totalItemQtd(it: ItemQtdValor): number {
  return it.incluir ? toNum(it.qtd) * toNum(it.valorUnit) : 0;
}

export function totalItensQtd(itens: ItemQtdValor[]): number {
  return itens.reduce((s, it) => s + totalItemQtd(it), 0);
}

export function totalGrupos(grupos: GrupoItens[]): number {
  return grupos.reduce((s, g) => s + totalItensQtd(g.itens), 0);
}

export function totalItemCusto(it: ItemCusto): number {
  return toNum(it.qtd) * toNum(it.valorUnit);
}

export function totalItensCusto(itens: ItemCusto[]): number {
  return itens.reduce((s, it) => s + totalItemCusto(it), 0);
}

// ---------- Equipe ----------

export function paxDe(o: Orcamento): number {
  return Math.max(0, toNum(o.evento.pax));
}

/** No modo "auto", a quantidade sai de convidados ÷ convidados por profissional. */
export function qtdFuncao(f: FuncaoEquipe, pax: number): number {
  if (f.auto) {
    const razao = toNum(f.profissionalPorPax);
    return razao > 0 ? Math.round(pax / razao) : toNum(f.qtd);
  }
  return toNum(f.qtd);
}

export function totalFuncao(f: FuncaoEquipe, pax: number): number {
  return qtdFuncao(f, pax) * toNum(f.diarias) * toNum(f.valorUnit);
}

export function tamanhoEquipe(o: Orcamento): number {
  const pax = paxDe(o);
  return o.equipe.reduce((s, f) => s + qtdFuncao(f, pax), 0);
}

// ---------- Cardápio ----------

export function baseCategoria(cat: CategoriaCardapio, o: Orcamento): number {
  return cat.baseQtd === "staff" ? tamanhoEquipe(o) : paxDe(o);
}

export function totalItemCardapio(it: ItemCardapio, base: number): number {
  return it.incluir ? toNum(it.valorUnit) * toNum(it.porcaoPax) * base : 0;
}

export function totalCategoria(cat: CategoriaCardapio, o: Orcamento): number {
  const base = baseCategoria(cat, o);
  return cat.itens.reduce((s, it) => s + totalItemCardapio(it, base), 0);
}

/** Gramas servidas por convidado (ignora a alimentação da equipe). */
export function gramasPorConvidado(o: Orcamento): number {
  return o.cardapio
    .filter((c) => c.baseQtd !== "staff")
    .flatMap((c) => c.itens)
    .reduce((s, it) => s + (it.incluir ? toNum(it.gramas) * toNum(it.porcaoPax) : 0), 0);
}

// ---------- Formação do preço ----------

/** Fator de "preço = custo ÷ (1 − %)", limitado para não dividir por ~0. */
function fator(pct: number): number {
  return Math.min(1, Math.max(0.02, 1 - toNum(pct) / 100));
}

export const INSUMOS: { chave: ChaveInsumo; label: string }[] = [
  { chave: "alimentos", label: "Alimentos" },
  { chave: "bebida", label: "Bebida" },
  { chave: "gelo", label: "Gelo" },
  { chave: "descartaveis", label: "Descartáveis" },
];

export interface ResumoOrcamento {
  pax: number;
  equipeQtd: number;
  alimentos: number;
  bebidas: number;
  gelo: number;
  descartaveis: number;
  equipe: number;
  materiais: number;
  extras: number;
  custo: number;
  /** Preço após a margem bruta (base de despesas/lucro). */
  vendaBase: number;
  /** Preço final, após comissão, outros e impostos. */
  venda: number;
  porPessoa: number;
  margemValor: number;
  margemPct: number;
  despesas: number;
  lucroBruto: number;
  investimento: number;
  lucro: number;
  insumos: Record<ChaveInsumo, { verba: number; utilizado: number; saldo: number }>;
  saldoInsumos: number;
  pacoteCusto: number;
  pacoteContrib: number;
  servicosContrib: number;
  lucroReal: number;
  gramas: number;
}

export function calcularResumo(o: Orcamento): ResumoOrcamento {
  const pax = paxDe(o);
  const alimentos = o.cardapio.reduce((s, c) => s + totalCategoria(c, o), 0);
  const bebidas = totalGrupos(o.bebidas);
  const gelo = totalItensQtd(o.gelo);
  const descartaveis = totalGrupos(o.descartaveis);
  const equipe = o.equipe.reduce((s, f) => s + totalFuncao(f, pax), 0);
  const materiais = toNum(o.materiais.frete) + toNum(o.materiais.verbaLocacao) + totalGrupos(o.materiais.grupos);
  const extras = totalItensCusto(o.verbaExtra) + totalItensCusto(o.degustacao);
  const custo = alimentos + equipe + materiais + extras;

  const m = o.margens;
  const vendaBase = custo / fator(m.margemBruta);
  const venda = vendaBase / fator(m.comissao) / fator(m.outros) / fator(m.impostos);
  const margemValor = vendaBase - custo;
  const lucroBruto = (vendaBase * toNum(m.margemLucroBruto)) / 100;
  const investimento = (vendaBase * toNum(m.margemInvestimento)) / 100;
  const lucro = lucroBruto - investimento;

  const utilizado: Record<ChaveInsumo, number> = { alimentos, bebida: bebidas, gelo, descartaveis };
  const insumos = {} as ResumoOrcamento["insumos"];
  let saldoInsumos = 0;
  for (const { chave } of INSUMOS) {
    const verba = toNum(o.verbaInsumos[chave]) * pax;
    const saldo = verba - utilizado[chave];
    insumos[chave] = { verba, utilizado: utilizado[chave], saldo };
    saldoInsumos += saldo;
  }

  const pacoteCusto = totalItensQtd(o.pacoteBebidas.itens);
  const pacoteContrib = toNum(o.pacoteBebidas.valorVendaPorPessoa) * pax - pacoteCusto;
  const servicosContrib = o.servicosAdicionais.reduce(
    (s, it) => s + toNum(it.valorCobrado) - toNum(it.custo),
    0
  );

  return {
    pax,
    equipeQtd: tamanhoEquipe(o),
    alimentos,
    bebidas,
    gelo,
    descartaveis,
    equipe,
    materiais,
    extras,
    custo,
    vendaBase,
    venda,
    porPessoa: venda / Math.max(1, pax),
    margemValor,
    margemPct: venda ? (margemValor / venda) * 100 : 0,
    despesas: (vendaBase * toNum(m.despesasFixas)) / 100,
    lucroBruto,
    investimento,
    lucro,
    insumos,
    saldoInsumos,
    pacoteCusto,
    pacoteContrib,
    servicosContrib,
    lucroReal: lucro + saldoInsumos + servicosContrib + pacoteContrib,
    gramas: gramasPorConvidado(o),
  };
}

/** Centavos, como o BigDecimal(15, 2) do backend. */
function centavos(v: number): number {
  return Math.round(v * 100) / 100;
}

/**
 * Monta o CreateOrcamentoRequest a partir do orçamento e do resumo já calculado.
 * Alimentos + equipe + degustação + outros = custo total do resumo.
 */
export function paraCreateOrcamentoRequest(o: Orcamento, r: ResumoOrcamento): CreateOrcamentoRequest {
  const e = o.evento;
  return {
    nomeCliente: e.cliente,
    contatoCliente: e.contato,
    tipoServico: e.tipoServico,
    duracaoEvento: toNum(e.duracaoHoras),
    numeroConvidados: Math.round(r.pax),
    valorAlimentos: centavos(r.alimentos),
    valorEquipe: centavos(r.equipe),
    valorDegustacao: centavos(totalItensCusto(o.degustacao)),
    valorOutros: centavos(r.materiais + totalItensCusto(o.verbaExtra)),
    valorTotal: centavos(r.venda),
    valorPorPessoa: centavos(r.porPessoa),
  };
}

/**
 * Pratos marcados no cardápio (os que entram no custo de alimentos), por categoria.
 */
export function pratosSelecionados(o: Orcamento): { categoria: string; pratos: string[] }[] {
  return o.cardapio
    .map((c) => ({
      categoria: c.categoria,
      pratos: c.itens.filter((it) => it.incluir && it.nome.trim()).map((it) => it.nome.trim()),
    }))
    .filter((c) => c.pratos.length > 0);
}

/** Faixa da margem de contribuição sobre a venda: ≥ 40% boa, ≥ 20% atenção, abaixo disso crítica. */
export function faixaMargem(pct: number): "boa" | "atencao" | "critica" {
  if (pct >= 40) return "boa";
  if (pct >= 20) return "atencao";
  return "critica";
}

// ---------- Atualização imutável de listas (edição das tabelas) ----------

export function substituirEm<T>(lista: T[], i: number, valor: T): T[] {
  return lista.map((x, j) => (j === i ? valor : x));
}

export function removerEm<T>(lista: T[], i: number): T[] {
  return lista.filter((_, j) => j !== i);
}
