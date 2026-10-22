import type { Lancamento } from "@/lib/models/financeiro";
import { fmtCurrency, fmtCurrencySigned } from "@/lib/utils/format";
import { resumoDre } from "@/lib/utils/financeiro";

/** Cartões de resumo (entradas, saídas, resultado) do período selecionado. */
export function FinanceiroResumo({ lancamentos }: { lancamentos: Lancamento[] }) {
  const { receitas, despesas, resultado } = resumoDre(lancamentos);

  return (
    <section className="fin-cards" aria-label="Resumo do período">
      <div className="fin-card">
        <div className="fin-label receita">▲ Entradas</div>
        <div className="fin-amount pos">{fmtCurrency(receitas)}</div>
      </div>
      <div className="fin-card">
        <div className="fin-label despesa">▼ Saídas</div>
        <div className="fin-amount neg">{fmtCurrency(despesas)}</div>
      </div>
      <div className="fin-card">
        <div className="fin-label">Resultado</div>
        <div className={`fin-amount ${resultado < 0 ? "neg" : ""}`}>{fmtCurrencySigned(resultado)}</div>
      </div>
    </section>
  );
}
