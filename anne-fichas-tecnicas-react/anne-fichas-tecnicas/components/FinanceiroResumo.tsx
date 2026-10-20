import type { Lancamento } from "@/lib/models/financeiro";
import { fmtCurrency } from "@/lib/utils/format";
import { resumoDre, totaisPorCategoria, type TotalCategoria } from "@/lib/utils/financeiro";

interface Props {
  lancamentos: Lancamento[];
}

function CategoriaColuna({
  titulo,
  tom,
  totais,
  vazio,
}: {
  titulo: string;
  tom: "receita" | "despesa";
  totais: TotalCategoria[];
  vazio: string;
}) {
  return (
    <div>
      <div className={`fin-label ${tom}`}>{titulo}</div>
      {totais.length === 0 ? (
        <p className="fin-muted">{vazio}</p>
      ) : (
        <dl className="fin-dre-list">
          {totais.map((c) => (
            <div key={c.categoria} className="fin-dre-row">
              <dt>{c.categoria}</dt>
              <dd>{fmtCurrency(c.total)}</dd>
            </div>
          ))}
        </dl>
      )}
    </div>
  );
}

/** Cartões de resumo (receitas, despesas, resultado) + DRE por categoria do período filtrado. */
export function FinanceiroResumo({ lancamentos }: Props) {
  const { receitas, despesas, resultado } = resumoDre(lancamentos);
  const receitasPorCategoria = totaisPorCategoria(lancamentos, "receita");
  const despesasPorCategoria = totaisPorCategoria(lancamentos, "despesa");

  return (
    <>
      <div className="fin-cards">
        <div className="fin-card">
          <div className="fin-label receita">▲ Receitas</div>
          <div className="fin-amount">{fmtCurrency(receitas)}</div>
        </div>
        <div className="fin-card">
          <div className="fin-label despesa">▼ Despesas</div>
          <div className="fin-amount">{fmtCurrency(despesas)}</div>
        </div>
        <div className="fin-card">
          <div className={`fin-label ${resultado >= 0 ? "receita" : "despesa"}`}>Resultado</div>
          <div className="fin-amount">{fmtCurrency(resultado)}</div>
        </div>
      </div>

      {(receitasPorCategoria.length > 0 || despesasPorCategoria.length > 0) && (
        <section className="fin-panel">
          <h2 className="section-h">DRE por categoria</h2>
          <div className="fin-dre-grid">
            <CategoriaColuna
              titulo="Receitas"
              tom="receita"
              totais={receitasPorCategoria}
              vazio="Sem receitas no período"
            />
            <CategoriaColuna
              titulo="Despesas"
              tom="despesa"
              totais={despesasPorCategoria}
              vazio="Sem despesas no período"
            />
          </div>
        </section>
      )}
    </>
  );
}
