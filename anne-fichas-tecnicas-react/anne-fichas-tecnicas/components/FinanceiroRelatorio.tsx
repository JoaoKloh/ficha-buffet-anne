import type { Lancamento } from "@/lib/models/financeiro";
import { fmtCurrency, fmtCurrencySigned, fmtDate } from "@/lib/utils/format";
import { resumoDre, totaisPorCategoria } from "@/lib/utils/financeiro";

/**
 * Relatório financeiro do período filtrado. Fica oculto na tela e é a única
 * coisa impressa nesta página (reusa os estilos de .orc-relatorio em globals.css).
 */
export function FinanceiroRelatorio({
  lancamentos,
  periodo,
}: {
  lancamentos: Lancamento[];
  periodo: string;
}) {
  const { receitas, despesas, resultado } = resumoDre(lancamentos);
  const receitasPorCategoria = totaisPorCategoria(lancamentos, "receita");
  const despesasPorCategoria = totaisPorCategoria(lancamentos, "despesa");
  const ordenados = [...lancamentos].sort(
    (a, b) => (b.data ?? "").localeCompare(a.data ?? "") || (b.id ?? 0) - (a.id ?? 0),
  );

  const linha = (rotulo: string, valor: string, forte = false) => (
    <tr key={rotulo}>
      <td>{forte ? <b>{rotulo}</b> : rotulo}</td>
      <td className="num">{forte ? <b>{valor}</b> : valor}</td>
    </tr>
  );

  return (
    <div className="orc-relatorio" aria-hidden="true">
      <div className="orc-relatorio-head">
        <div>
          <h1 className="page-title">Relatório financeiro</h1>
          <div className="fin-muted">Período: {periodo}</div>
        </div>
        <div className="orc-relatorio-marca">
          <div className="mark">ANNE</div>
          <div className="fin-muted">{lancamentos.length} lançamentos</div>
        </div>
      </div>

      <h2 className="section-h">Resumo</h2>
      <table className="orc-relatorio-table">
        <tbody>
          {linha("Receitas", fmtCurrency(receitas))}
          {linha("Despesas", fmtCurrency(despesas))}
          {linha("Resultado", fmtCurrencySigned(resultado), true)}
        </tbody>
      </table>

      {receitasPorCategoria.length > 0 && (
        <>
          <h2 className="section-h">Receitas por categoria</h2>
          <table className="orc-relatorio-table">
            <tbody>{receitasPorCategoria.map((c) => linha(c.categoria, fmtCurrency(c.total)))}</tbody>
          </table>
        </>
      )}

      {despesasPorCategoria.length > 0 && (
        <>
          <h2 className="section-h">Despesas por categoria</h2>
          <table className="orc-relatorio-table">
            <tbody>{despesasPorCategoria.map((c) => linha(c.categoria, fmtCurrency(c.total)))}</tbody>
          </table>
        </>
      )}

      <h2 className="section-h">Lançamentos</h2>
      {ordenados.length === 0 ? (
        <p className="fin-muted">Nenhum lançamento neste período.</p>
      ) : (
        <table className="orc-relatorio-table">
          <tbody>
            {ordenados.map((l, index) => (
              <tr key={l.id ?? `lancamento-${index}`}>
                <td>{fmtDate(l.data)}</td>
                <td>{l.descricao}</td>
                <td>{l.categoria}</td>
                <td className="num">
                  {l.tipo === "receita" ? "+" : "−"}
                  {fmtCurrency(l.valor)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
