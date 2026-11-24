import { addDays, addMonths, round2, sum } from "./datas";

/**
 * Regras puras da nota fiscal, usadas pelo BFF ao gravar e pelo formulário
 * para mostrar os totais enquanto a pessoa digita (mesma função, sem cópia).
 */

/** Quantidade da nota na unidade de estoque: em "compra", multiplica pelo fator da embalagem. */
export function qtdConvertida(qtdNota: number, unid: "estoque" | "compra", fatorCompra: number | null | undefined): number {
  const f = unid === "compra" && Number(fatorCompra) > 0 ? Number(fatorCompra) : 1;
  return Math.round((Number(qtdNota) || 0) * f * 1000) / 1000;
}

export const somaItens = (itens: readonly { valor: number }[]): number => round2(sum(itens, (it) => it.valor));
export const totalNf = (itens: readonly { valor: number }[], acrescimo: number): number => round2(somaItens(itens) + (Number(acrescimo) || 0));
export const somaParcelas = (parcelas: readonly { valor: number }[]): number => round2(sum(parcelas, (p) => p.valor));

/** Divide um total em n parcelas iguais com 2 casas; o resto vai para a última. */
export function dividirParcelas(total: number, n: number): number[] {
  const base = Math.floor((total / n) * 100) / 100;
  return Array.from({ length: n }, (_, i) => (i === n - 1 ? Math.round((total - base * (n - 1)) * 100) / 100 : base));
}

/** n parcelas a partir do 1º vencimento: intervalo em dias ou "m" (mensal, mesmo dia). */
export function gerarParcelas(total: number, n: number, primeiroVenc: string, intervalo: string, pagas: boolean) {
  return dividirParcelas(total, n).map((valor, i) => ({ venc: intervalo === "m" ? addMonths(primeiroVenc, i) : addDays(primeiroVenc, i * (Number(intervalo) || 30)), valor, pago: pagas }));
}

/** Confere quantidade × V. unitário com o V. total da linha (tolerância de 1 centavo). */
export function conferirLinha(qtdNota: number, valorUnit: number | null, valor: number): { esperado: number; ok: boolean } | null {
  if (!(Number(valorUnit) > 0) || !qtdNota) return null;
  const esperado = qtdNota * Number(valorUnit);
  return { esperado, ok: Math.abs(esperado - valor) < 0.011 };
}
