import { MESES_EXTENSO, moverPeriodo, type Periodo } from "@/lib/utils/financeiro";

/** Régua do período: mês anterior/próximo (ou ano, com "Ver o ano inteiro" marcado). */
export function FinanceiroPeriodo({ value, onChange }: { value: Periodo; onChange: (p: Periodo) => void }) {
  const unidade = value.anoInteiro ? "ano" : "mês";

  return (
    <div className="fin-periodo">
      <div className="fin-periodo-bar">
        <button
          type="button"
          className="btn ghost small"
          aria-label={value.anoInteiro ? "Ano anterior" : "Mês anterior"}
          title={`Voltar um ${unidade}`}
          onClick={() => onChange(moverPeriodo(value, -1))}
        >
          ‹
        </button>
        <div className="fin-periodo-nome" aria-live="polite">
          {value.anoInteiro ? `Ano de ${value.ano}` : MESES_EXTENSO[value.mes]}
          <small>{value.anoInteiro ? "todos os meses" : value.ano}</small>
        </div>
        <button
          type="button"
          className="btn ghost small"
          aria-label={value.anoInteiro ? "Próximo ano" : "Próximo mês"}
          title={`Avançar um ${unidade}`}
          onClick={() => onChange(moverPeriodo(value, 1))}
        >
          ›
        </button>
      </div>
      <label className="fin-periodo-ano">
        <input
          type="checkbox"
          checked={value.anoInteiro}
          onChange={(e) => onChange({ ...value, anoInteiro: e.target.checked })}
        />
        Ver o ano inteiro
      </label>
    </div>
  );
}
