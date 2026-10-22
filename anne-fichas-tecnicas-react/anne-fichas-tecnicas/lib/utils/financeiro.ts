import type { Lancamento, TipoLancamento } from "@/lib/models/financeiro";
import {
  CATEGORIAS_POR_TIPO,
  GRUPOS_DRE,
  PLANO_DE_CONTAS,
  grupoDaCategoria,
  type GrupoDre,
} from "@/lib/models/financeiro";

const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

export function somar(lancamentos: Lancamento[]): number {
  return lancamentos.reduce((s, l) => s + l.valor, 0);
}

export interface TotalCategoria {
  categoria: string;
  total: number;
}

/** Totais por categoria do tipo, na ordem das categorias, omitindo as zeradas. */
export function totaisPorCategoria(lancamentos: Lancamento[], tipo: TipoLancamento): TotalCategoria[] {
  const doTipo = lancamentos.filter((l) => l.tipo === tipo);
  // Categorias fora do plano de contas (lançamentos antigos) entram no fim.
  const categorias = Array.from(new Set([...CATEGORIAS_POR_TIPO[tipo], ...doTipo.map((l) => l.categoria)]));
  return categorias
    .map((categoria) => ({
      categoria,
      total: somar(doTipo.filter((l) => l.categoria === categoria)),
    }))
    .filter((c) => c.total > 0);
}

export interface ResumoDre {
  receitas: number;
  despesas: number;
  resultado: number;
}

export function resumoDre(lancamentos: Lancamento[]): ResumoDre {
  const receitas = somar(lancamentos.filter((l) => l.tipo === "receita"));
  const despesas = somar(lancamentos.filter((l) => l.tipo === "despesa"));
  return { receitas, despesas, resultado: receitas - despesas };
}

// ---------- Período (régua de mês / ano inteiro) ----------

export const MESES_EXTENSO = [
  "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
  "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
];

export interface Periodo {
  ano: number;
  mes: number; // 0–11
  anoInteiro: boolean;
}

export function periodoAtual(): Periodo {
  const hoje = new Date();
  return { ano: hoje.getFullYear(), mes: hoje.getMonth(), anoInteiro: false };
}

/** (2026, 8) -> "2026-09" */
export function chaveMes(ano: number, mes: number): string {
  return `${ano}-${String(mes + 1).padStart(2, "0")}`;
}

/** Avança (+1) ou recua (−1) um mês, ou um ano quando o período é o ano inteiro. */
export function moverPeriodo(p: Periodo, passo: 1 | -1): Periodo {
  if (p.anoInteiro) return { ...p, ano: p.ano + passo };
  const total = p.ano * 12 + p.mes + passo;
  return { ...p, ano: Math.floor(total / 12), mes: ((total % 12) + 12) % 12 };
}

export function rotuloPeriodo(p: Periodo): string {
  return p.anoInteiro ? `ano de ${p.ano}` : `${MESES_EXTENSO[p.mes]} ${p.ano}`;
}

/** Lançamentos cuja data inicial cai no período. */
export function noPeriodo(lancamentos: Lancamento[], p: Periodo): Lancamento[] {
  const prefixo = p.anoInteiro ? `${p.ano}-` : chaveMes(p.ano, p.mes);
  return lancamentos.filter((l) => (l.data ?? "").startsWith(prefixo));
}

// ---------- DRE ----------

/** [mínimo, máximo, texto] em fração da receita líquida. */
export type Referencia = [number | null, number | null, string];

export type LinhaDre =
  | { tipo: "grupo"; rotulo: string; valor: number; referencia?: Referencia }
  | { tipo: "item"; rotulo: string; categoria: string; valor: number }
  | { tipo: "total"; rotulo: string; valor: number; referencia?: Referencia; final?: boolean };

export interface Dre {
  linhas: LinhaDre[];
  receitaBruta: number;
  receitaLiquida: number;
  custosDiretos: number;
  lucroBruto: number;
  margemContribuicao: number;
  despesasFixas: number;
  ebitda: number;
  resultado: number; // lucro líquido
}

/** Categorias de um grupo: as do plano de contas e, depois, as antigas que caem nele. */
function categoriasDoGrupo(base: Lancamento[], grupo: GrupoDre): { nome: string; tipo: TipoLancamento }[] {
  const doPlano = PLANO_DE_CONTAS.filter((c) => c.grupo === grupo);
  const legadas = new Map<string, TipoLancamento>();
  for (const l of base) {
    const noPlano = PLANO_DE_CONTAS.some((c) => c.nome === l.categoria);
    if (!noPlano && grupoDaCategoria(l.categoria, l.tipo) === grupo) legadas.set(l.categoria, l.tipo);
  }
  const extras = Array.from(legadas, ([nome, tipo]) => ({ nome, tipo })).sort((x, y) =>
    x.nome.localeCompare(y.nome),
  );
  return [...doPlano, ...extras];
}

/** `base` define as linhas (categorias); por padrão, os próprios lançamentos. */
export function montarDre(lancamentos: Lancamento[], base: Lancamento[] = lancamentos): Dre {
  const itens = (grupo: GrupoDre) =>
    categoriasDoGrupo(base, grupo).map((c) => ({
      tipo: "item" as const,
      rotulo: c.nome,
      categoria: c.nome,
      tipoLancamento: c.tipo,
      valor: somar(lancamentos.filter((l) => l.categoria === c.nome && l.tipo === c.tipo)),
    }));
  // Soma com sinal: receitas positivas, despesas negativas (só importa no grupo misto "oth").
  const saldo = (linhas: ReturnType<typeof itens>) =>
    linhas.reduce((s, l) => s + (l.tipoLancamento === "receita" ? l.valor : -l.valor), 0);
  const semTipo = (linhas: ReturnType<typeof itens>) =>
    linhas.map(({ tipoLancamento: _, ...l }) => l);

  const rb = itens("rb"), ded = itens("ded"), cd = itens("cd"), dv = itens("dv"), df = itens("df"), oth = itens("oth");
  const receitaBruta = saldo(rb);
  const deducoes = -saldo(ded);
  const receitaLiquida = receitaBruta - deducoes;
  const custosDiretos = -saldo(cd);
  const lucroBruto = receitaLiquida - custosDiretos;
  const despesasVariaveis = -saldo(dv);
  const margemContribuicao = lucroBruto - despesasVariaveis;
  const despesasFixas = -saldo(df);
  const ebitda = margemContribuicao - despesasFixas;
  const outros = saldo(oth); // receitas financeiras − depreciação, despesas financeiras e IR
  const resultado = ebitda + outros;

  const linhas: LinhaDre[] = [
    { tipo: "grupo", rotulo: GRUPOS_DRE.rb, valor: receitaBruta },
    ...semTipo(rb),
    { tipo: "grupo", rotulo: `(–) ${GRUPOS_DRE.ded}`, valor: deducoes },
    ...semTipo(ded),
    { tipo: "total", rotulo: "= Receita líquida", valor: receitaLiquida },
    { tipo: "grupo", rotulo: `(–) ${GRUPOS_DRE.cd}`, valor: custosDiretos, referencia: [null, 0.55, "até 55%"] },
    ...semTipo(cd),
    { tipo: "total", rotulo: "= Lucro bruto", valor: lucroBruto, referencia: [0.45, null, "acima de 45%"] },
    { tipo: "grupo", rotulo: `(–) ${GRUPOS_DRE.dv}`, valor: despesasVariaveis, referencia: [null, 0.1, "até 10%"] },
    ...semTipo(dv),
    { tipo: "total", rotulo: "= Margem de contribuição", valor: margemContribuicao },
    { tipo: "grupo", rotulo: `(–) ${GRUPOS_DRE.df}`, valor: despesasFixas, referencia: [null, 0.35, "até 35%"] },
    ...semTipo(df),
    { tipo: "total", rotulo: "= EBITDA", valor: ebitda, referencia: [0.12, null, "12–25%"] },
    { tipo: "grupo", rotulo: GRUPOS_DRE.oth, valor: outros },
    ...semTipo(oth),
    { tipo: "total", rotulo: "= Lucro líquido", valor: resultado, referencia: [0.1, null, "10–20%"], final: true },
  ];

  return {
    linhas,
    receitaBruta,
    receitaLiquida,
    custosDiretos,
    lucroBruto,
    margemContribuicao,
    despesasFixas,
    ebitda,
    resultado,
  };
}

/** Situação de um percentual frente à faixa de referência: dentro, perto do limite ou fora. */
export function situacaoReferencia(ref: Referencia, fracao: number): "boa" | "atencao" | "critica" {
  const [min, max] = ref;
  if (max !== null) return fracao <= max ? "boa" : fracao <= max * 1.15 ? "atencao" : "critica";
  return fracao >= (min ?? 0) ? "boa" : fracao >= 0 ? "atencao" : "critica";
}

/**
 * DRE do ano, um mês por coluna + total, em texto separado por tabulação
 * (cola direto no Excel). Valores com vírgula decimal.
 */
export function dreAnualTabulada(lancamentos: Lancamento[], ano: number): string {
  const doAno = noPeriodo(lancamentos, { ano, mes: 0, anoInteiro: true });
  const porMes = Array.from({ length: 12 }, (_, mes) =>
    montarDre(noPeriodo(doAno, { ano, mes, anoInteiro: false }), doAno),
  );
  const anual = montarDre(doAno);
  const numero = (v: number) => (Math.round(v * 100) / 100).toString().replace(".", ",");
  const cabecalho = [`Conta ${ano}`, ...MESES, "Total"].join("\t");
  const linhas = anual.linhas.map((linha, i) => {
    const valores = [...porMes, anual].map((dre) => {
      const l = dre.linhas[i];
      return l ? numero(l.valor) : "";
    });
    return [linha.rotulo, ...valores].join("\t");
  });
  return [cabecalho, ...linhas].join("\n");
}
