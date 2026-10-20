import type { Lancamento } from "@/lib/models/financeiro";
import { fmtCurrency, fmtDate } from "@/lib/utils/format";

interface Props {
  lancamentos: Lancamento[];
  onEdit: (l: Lancamento) => void;
  onDelete: (l: Lancamento) => void;
}

export function LancamentoList({ lancamentos, onEdit, onDelete }: Props) {
  const ordenados = [...lancamentos].sort((a, b) => {
    const dataA = a.data ?? "";
    const dataB = b.data ?? "";
    const idA = a.id ?? 0;
    const idB = b.id ?? 0;

    return dataB.localeCompare(dataA) || idB - idA;
  });

  return (
    <section className="fin-panel fin-list-panel">
      <h2 className="section-h">Lançamentos</h2>
      {ordenados.length === 0 ? (
        <p className="fin-muted">Nenhum lançamento neste período ainda.</p>
      ) : (
        <ul className="fin-list">
          {ordenados.map((l, index) => (
            <li key={l.id ?? `lancamento-${index}-${l.data || ""}`} className="fin-row">
              <span className={`fin-dot ${l.tipo}`} aria-hidden="true" />
              <div className="fin-row-body">
                <div className="fin-row-desc">{l.descricao}</div>
                <div className="prod-meta">
                  {l.categoria} · {fmtDate(l.data)}
                </div>
              </div>
              <span className={`fin-row-valor ${l.tipo}`}>
                {l.tipo === "receita" ? "+" : "−"}
                {fmtCurrency(l.valor)}
              </span>
              <div className="fin-row-actions">
                <button
                  type="button"
                  className="btn small ghost icon-btn"
                  onClick={() => onEdit(l)}
                  aria-label={`Editar lançamento ${l.descricao}`}
                  title="Editar lançamento"
                >
                  <svg
                    width="13"
                    height="13"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
                  </svg>
                  <span className="fin-action-label">Editar</span>
                </button>
                <button
                  type="button"
                  className="btn small danger icon-btn"
                  onClick={() => onDelete(l)}
                  aria-label={`Excluir lançamento ${l.descricao}`}
                  title="Excluir lançamento"
                >
                  <svg
                    width="13"
                    height="13"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M3 6h18" />
                    <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                  </svg>
                  <span className="fin-action-label">Excluir</span>
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}