import type { Lancamento, TipoLancamento } from "@/lib/models/financeiro";
import { CATEGORIAS_POR_TIPO } from "@/lib/models/financeiro";

const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

/** "2026-09-24" -> "2026-09" */
export function mesDe(data?: string | null): string {
  if (!data || typeof data !== "string") {
    return "";}
  return data.slice(0, 7);
}

/** "2026-09" -> "Set/2026" */
export function fmtMes(mes: string): string {
  const [ano, m] = mes.split("-");
  const nome = MESES[parseInt(m ?? "", 10) - 1];
  return nome ? `${nome}/${ano}` : mes;
}

/** Meses com lançamentos, do mais recente para o mais antigo. */
export function mesesDisponiveis(lancamentos: Lancamento[]) {
  return Array.from(
    new Set(
      lancamentos
        .map((item) => mesDe(item.data))
        .filter(Boolean) // Remove strings vazias ("")
    )
  ).sort().reverse();
}

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
  return CATEGORIAS_POR_TIPO[tipo]
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
