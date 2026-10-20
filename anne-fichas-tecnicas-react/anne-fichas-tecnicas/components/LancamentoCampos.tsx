"use client";

import { ApiError } from "@/lib/api/client";
import {
  CATEGORIAS_POR_TIPO,
  CriarLancamentoSchema,
  type CriarLancamentoDTO,
  type Lancamento,
  type TipoLancamento,
} from "@/lib/models/financeiro";

/** Estado dos campos: `valor` fica como texto enquanto o usuário digita. */
export interface LancamentoCamposState {
  tipo: TipoLancamento;
  categoria: string;
  descricao: string;
  valor: string;
  data: string;
}

// Data local (não UTC): à noite, toISOString() já cairia no dia seguinte.
function hojeIso(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

export function camposVazios(): LancamentoCamposState {
  return { tipo: "receita", categoria: CATEGORIAS_POR_TIPO.receita[0], descricao: "", valor: "", data: hojeIso() };
}

export function camposDe(l: Lancamento): LancamentoCamposState {
  return { tipo: l.tipo, categoria: l.categoria, descricao: l.descricao, valor: String(l.valor), data: l.data };
}

/** Valida com as mesmas regras do CriarLancamentoDTO do backend (ver lib/models/financeiro.ts). */
export function validarCampos(
  campos: LancamentoCamposState
): { ok: true; data: CriarLancamentoDTO } | { ok: false; erros: string[] } {
  const parsed = CriarLancamentoSchema.safeParse({
    ...campos,
    valor: campos.valor === "" ? undefined : Number(campos.valor),
  });
  return parsed.success
    ? { ok: true, data: parsed.data }
    : { ok: false, erros: parsed.error.issues.map((i) => i.message) };
}

/** Mensagem do backend (ex.: Bean Validation) quando houver; senão, o texto genérico. */
export function mensagemDeErro(err: unknown, fallback: string): string {
  return err instanceof ApiError ? err.message : fallback;
}

interface Props {
  idPrefix: string;
  value: LancamentoCamposState;
  onChange: (next: LancamentoCamposState) => void;
}

export function LancamentoCampos({ idPrefix, value, onChange }: Props) {
  const id = (campo: string) => `${idPrefix}-${campo}`;
  const categorias: readonly string[] = CATEGORIAS_POR_TIPO[value.tipo];
  // Um lançamento salvo com categoria fora da lista atual continua editável sem perder o valor.
  const opcoes = categorias.includes(value.categoria) ? categorias : [value.categoria, ...categorias];

  function trocarTipo(tipo: TipoLancamento) {
    if (tipo === value.tipo) return;
    onChange({ ...value, tipo, categoria: CATEGORIAS_POR_TIPO[tipo][0] });
  }

  return (
    <div className="fin-form-grid">
      <div className="field-block">
        <label id={id("tipo-label")}>Tipo</label>
        <div className="segmented" role="group" aria-labelledby={id("tipo-label")}>
          <button
            type="button"
            className={value.tipo === "receita" ? "active receita" : ""}
            aria-pressed={value.tipo === "receita"}
            onClick={() => trocarTipo("receita")}
          >
            Receita
          </button>
          <button
            type="button"
            className={value.tipo === "despesa" ? "active despesa" : ""}
            aria-pressed={value.tipo === "despesa"}
            onClick={() => trocarTipo("despesa")}
          >
            Despesa
          </button>
        </div>
      </div>

      <div className="field-block">
        <label htmlFor={id("categoria")}>Categoria</label>
        <select
          id={id("categoria")}
          value={value.categoria}
          onChange={(e) => onChange({ ...value, categoria: e.target.value })}
        >
          {opcoes.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      <div className="field-block">
        <label htmlFor={id("descricao")}>Descrição</label>
        <input
          id={id("descricao")}
          type="text"
          placeholder="Ex.: Casamento Silva"
          value={value.descricao}
          onChange={(e) => onChange({ ...value, descricao: e.target.value })}
        />
      </div>

      <div className="field-block">
        <label htmlFor={id("valor")}>Valor (R$)</label>
        <input
          id={id("valor")}
          type="number"
          inputMode="decimal"
          step="0.01"
          min="0"
          placeholder="0,00"
          value={value.valor}
          onChange={(e) => onChange({ ...value, valor: e.target.value })}
        />
      </div>

      <div className="field-block">
        <label htmlFor={id("data")}>Data</label>
        <input
          id={id("data")}
          type="date"
          value={value.data}
          onChange={(e) => onChange({ ...value, data: e.target.value })}
        />
      </div>
    </div>
  );
}
