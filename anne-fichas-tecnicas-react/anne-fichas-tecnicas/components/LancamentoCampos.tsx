"use client";

import { ApiError } from "@/lib/api/client";
import { fmtDate } from "@/lib/utils/format";
import {
  CATEGORIAS_POR_TIPO,
  CriarLancamentoSchema,
  categoriasAgrupadas,
  type CriarLancamentoDTO,
  type Lancamento,
  type TipoLancamento,
} from "@/lib/models/financeiro";
import type { ProducaoResponseDTO } from "@/lib/models/producao";

/** Estado dos campos: `valor` fica como texto enquanto o usuário digita. */
export interface LancamentoCamposState {
  tipo: TipoLancamento;
  categoria: string;
  descricao: string;
  valor: string;
  data: string;
  dataFinal: string;
  producaoIds: number[];
}

// Data local (não UTC): à noite, toISOString() já cairia no dia seguinte.
function hojeIso(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

export function camposVazios(): LancamentoCamposState {
  const hoje = hojeIso();
  return {
    tipo: "receita",
    categoria: "",
    descricao: "",
    valor: "",
    data: hoje,
    dataFinal: hoje,
    producaoIds: [],
  };
}

export function camposDe(l: Lancamento): LancamentoCamposState {
  return {
    tipo: l.tipo,
    categoria: l.categoria,
    descricao: l.descricao,
    valor: String(l.valor),
    data: l.data,
    dataFinal: l.dataFinal ?? l.data,
    producaoIds: l.producaoIds ?? [],
  };
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
  producoes: ProducaoResponseDTO[];
}

export function LancamentoCampos({ idPrefix, value, onChange, producoes }: Props) {
  const id = (campo: string) => `${idPrefix}-${campo}`;
  // Um lançamento salvo com categoria fora da lista atual continua editável sem perder o valor.
  const categoriaLegada =
    value.categoria && !CATEGORIAS_POR_TIPO[value.tipo].includes(value.categoria) ? value.categoria : null;

  function trocarTipo(tipo: TipoLancamento) {
    if (tipo === value.tipo) return;
    onChange({ ...value, tipo, categoria: "" });
  }

  function alternarProducao(producaoId: number) {
    const producaoIds = value.producaoIds.includes(producaoId)
      ? value.producaoIds.filter((pid) => pid !== producaoId)
      : [...value.producaoIds, producaoId];
    onChange({ ...value, producaoIds });
  }

  return (
    <div className="fin-form-grid">
      <div className="field-block fin-span">
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
        <label htmlFor={id("data")}>Data inicial</label>
        <input
          id={id("data")}
          type="date"
          value={value.data}
          onChange={(e) => onChange({ ...value, data: e.target.value })}
        />
      </div>

      <div className="field-block">
        <label htmlFor={id("dataFinal")}>Data final</label>
        <input
          id={id("dataFinal")}
          type="date"
          min={value.data}
          value={value.dataFinal}
          onChange={(e) => onChange({ ...value, dataFinal: e.target.value })}
        />
      </div>

      <div className="field-block fin-valor">
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
        <label htmlFor={id("categoria")}>Categoria</label>
        <select
          id={id("categoria")}
          value={value.categoria}
          onChange={(e) => onChange({ ...value, categoria: e.target.value })}
        >
          <option value="">Escolha a categoria</option>
          {categoriaLegada && <option value={categoriaLegada}>{categoriaLegada}</option>}
          {categoriasAgrupadas(value.tipo).map(([grupo, categorias]) => (
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
      </div>

      <div className="field-block fin-span">
        <label htmlFor={id("descricao")}>Descrição</label>
        <input
          id={id("descricao")}
          type="text"
          placeholder="Ex.: Casamento Silva"
          value={value.descricao}
          onChange={(e) => onChange({ ...value, descricao: e.target.value })}
        />
      </div>

      <div className="field-block fin-producoes" role="group" aria-labelledby={id("producoes-label")}>
        <label id={id("producoes-label")}>Produções vinculadas (opcional)</label>
        {producoes.length === 0 ? (
          <p className="fin-muted">Nenhuma produção cadastrada.</p>
        ) : (
          <div className="assoc-list">
            {producoes.map((p) => (
              <label key={p.id} className="assoc-row">
                <input
                  type="checkbox"
                  checked={value.producaoIds.includes(p.id)}
                  onChange={() => alternarProducao(p.id)}
                />
                <span>
                  <span className="assoc-name">{p.nome}</span>
                  <span className="assoc-meta">{fmtDate(p.data)}</span>
                </span>
              </label>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
