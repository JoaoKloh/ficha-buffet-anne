import type { Lancamento } from "@/lib/models/financeiro";
import { PLANO_DE_CONTAS, categoriasAgrupadas } from "@/lib/models/financeiro";
import type { ProducaoResponseDTO } from "@/lib/models/producao";
import { fmtCurrency, fmtCurrencySigned } from "@/lib/utils/format";
import { MESES_EXTENSO } from "@/lib/utils/financeiro";

/** "2026-09-24" -> "24 de setembro" */
function fmtDia(iso: string): string {
  const [, m, d] = (iso ?? "").split("-");
  const mes = MESES_EXTENSO[parseInt(m ?? "", 10) - 1];
  return mes && d ? `${parseInt(d, 10)} de ${mes.toLowerCase()}` : iso;
}

export function ordenarLancamentos(lancamentos: Lancamento[]): Lancamento[] {
  return [...lancamentos].sort(
    (a, b) => (b.data ?? "").localeCompare(a.data ?? "") || (b.id ?? 0) - (a.id ?? 0),
  );
}

interface RowProps {
  lancamento: Lancamento;
  nomesProducoes: Map<number, string>;
  onEdit: (l: Lancamento) => void;
  onDelete: (l: Lancamento) => void;
}

export function LancamentoRow({ lancamento: l, nomesProducoes, onEdit, onDelete }: RowProps) {
  const producoes = (l.producaoIds ?? []).map((id) => nomesProducoes.get(id)).filter(Boolean);

  return (
    <li className="fin-row">
      <span className={`fin-dot ${l.tipo}`} aria-hidden="true" />
      <div className="fin-row-body">
        <div className="fin-row-desc">{l.descricao}</div>
        <div className="prod-meta">
          {fmtDia(l.data)} · {l.categoria}
          {producoes.length > 0 && ` · ${producoes.join(", ")}`}
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
  );
}

export interface FiltrosLancamento {
  categoria: string; // "" = todas
  producaoId: string; // "" = todas
}

interface Props {
  /** Lançamentos do período (antes dos filtros desta aba). */
  lancamentos: Lancamento[];
  producoes: ProducaoResponseDTO[];
  filtros: FiltrosLancamento;
  onFiltros: (f: FiltrosLancamento) => void;
  onEdit: (l: Lancamento) => void;
  onDelete: (l: Lancamento) => void;
  /** Imprime o relatório financeiro da produção selecionada no filtro. */
  onImprimirProducao: (p: ProducaoResponseDTO) => void;
}

export function LancamentoList({
  lancamentos,
  producoes,
  filtros,
  onFiltros,
  onEdit,
  onDelete,
  onImprimirProducao,
}: Props) {
  const nomesProducoes = new Map(producoes.map((p) => [p.id, p.nome]));
  // Só oferece as produções que aparecem em algum lançamento do período.
  const idsNoPeriodo = new Set(lancamentos.flatMap((l) => l.producaoIds ?? []));
  const producoesNoPeriodo = producoes.filter((p) => idsNoPeriodo.has(p.id));
  // Categorias fora do plano de contas (lançamentos antigos) continuam filtráveis.
  const categoriasLegadas = Array.from(
    new Set(
      lancamentos
        .map((l) => l.categoria)
        .filter((c) => !PLANO_DE_CONTAS.some((p) => p.nome === c)),
    ),
  ).sort();
  const producaoAtiva = idsNoPeriodo.has(Number(filtros.producaoId)) ? filtros.producaoId : "";
  const producaoSelecionada = producoesNoPeriodo.find((p) => String(p.id) === producaoAtiva);

  const filtrados = ordenarLancamentos(
    lancamentos.filter(
      (l) =>
        (!filtros.categoria || l.categoria === filtros.categoria) &&
        (!producaoAtiva || (l.producaoIds ?? []).includes(Number(producaoAtiva))),
    ),
  );
  const saldo = filtrados.reduce((s, l) => s + (l.tipo === "receita" ? l.valor : -l.valor), 0);

  // Agrupa por dia mantendo a ordem (mais recente primeiro).
  const dias: { dia: string; itens: Lancamento[] }[] = [];
  for (const l of filtrados) {
    const ultimo = dias[dias.length - 1];
    if (ultimo && ultimo.dia === l.data) ultimo.itens.push(l);
    else dias.push({ dia: l.data, itens: [l] });
  }

  return (
    <section className="fin-panel fin-list-panel">
      <div className="fin-filtros">
        <select
          aria-label="Filtrar por categoria"
          value={filtros.categoria}
          onChange={(e) => onFiltros({ ...filtros, categoria: e.target.value })}
        >
          <option value="">Todas as categorias</option>
          {categoriasLegadas.length > 0 && (
            <optgroup label="Categorias antigas">
              {categoriasLegadas.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </optgroup>
          )}
          {categoriasAgrupadas().map(([grupo, categorias]) => (
            <optgroup key={grupo} label={grupo}>
              {categorias.map((c) => (
                <option key={c.nome} value={c.nome}>
                  {c.nome}
                  {c.dica && ` (${c.dica})`}
                </option>
              ))}
            </optgroup>
          ))}
        </select>
        <select
          aria-label="Filtrar por produção vinculada"
          value={producaoAtiva}
          onChange={(e) => onFiltros({ ...filtros, producaoId: e.target.value })}
        >
          <option value="">Todas as produções</option>
          {producoesNoPeriodo.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nome}
            </option>
          ))}
        </select>
        {producaoSelecionada && (
          <button
            type="button"
            className="btn ghost small"
            onClick={() => onImprimirProducao(producaoSelecionada)}
          >
            Imprimir relatório da produção
          </button>
        )}
      </div>

      {filtrados.length === 0 ? (
        <p className="empty-state">
          {lancamentos.length > 0
            ? "Nada com esses filtros."
            : "Nenhum lançamento neste período. Use a aba Lançar para registrar o primeiro."}
        </p>
      ) : (
        <>
          <div className="fin-list-head">
            <span className="fin-muted">
              {filtrados.length} lançamento{filtrados.length > 1 ? "s" : ""}
            </span>
            <span className={`fin-row-valor ${saldo < 0 ? "despesa" : "receita"}`}>
              Saldo {fmtCurrencySigned(saldo)}
            </span>
          </div>
          {dias.map(({ dia, itens }) => (
            <div key={dia}>
              <div className="fin-dia">{fmtDia(dia)}</div>
              <ul className="fin-list">
                {itens.map((l, index) => (
                  <LancamentoRow
                    key={l.id ?? `lancamento-${index}-${l.data || ""}`}
                    lancamento={l}
                    nomesProducoes={nomesProducoes}
                    onEdit={onEdit}
                    onDelete={onDelete}
                  />
                ))}
              </ul>
            </div>
          ))}
        </>
      )}
    </section>
  );
}
