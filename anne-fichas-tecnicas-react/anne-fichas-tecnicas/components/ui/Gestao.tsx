"use client";

import { useState } from "react";
import { iconeKpi } from "./Icones";
import { MESES } from "@/lib/utils/numero";
import { useSheet } from "./Sheet";
import { useToast } from "./Toast";

/** Peças visuais do protótipo usadas por todas as telas do Anne Gestão. */

export function Kpi({ k, v, h, negativo }: { k: string; v: React.ReactNode; h: React.ReactNode; negativo?: boolean }) {
  // O tamanho do valor acompanha a largura do cartão: `--n` é o número de caracteres (ver `.kpi .v`).
  const texto = typeof v === "string" ? v : undefined;
  return (
    <div className="kpi">
      <div className="kt">
        <span className="k">{k}</span>
        <span className="ic" aria-hidden="true">
          {iconeKpi(k)}
        </span>
      </div>
      <span className={`v${negativo ? " neg" : ""}`} title={texto} style={texto ? ({ "--n": Math.max(texto.length, 6) } as React.CSSProperties) : undefined}>
        {v}
      </span>
      <span className="h">{h}</span>
    </div>
  );
}

export function Pill({ tom = "", children }: { tom?: string; children: React.ReactNode }) {
  return <span className={`pill ${tom}`}>{children}</span>;
}

export function Datebox({ iso }: { iso: string | null | undefined }) {
  if (!iso)
    return (
      <div className="datebox">
        <b>—</b>
        <span>sem data</span>
      </div>
    );
  return (
    <div className="datebox">
      <b>{iso.slice(8, 10)}</b>
      <span>{MESES[Number(iso.slice(5, 7)) - 1]}</span>
    </div>
  );
}

export function Vazio({ titulo, children }: { titulo?: string; children?: React.ReactNode }) {
  return (
    <div className="empty">
      {titulo && <b>{titulo}</b>}
      {children}
    </div>
  );
}

export function Carregando({ erro }: { erro?: string | null }) {
  return <div className="empty">{erro ? <span className="neg">{erro}</span> : "Carregando…"}</div>;
}

export function Chips<K extends string>({ opcoes, valor, onChange, rotulo }: { opcoes: readonly (readonly [K, string])[]; valor: K; onChange: (k: K) => void; rotulo?: string }) {
  return (
    <div className="toolbar" role="group" aria-label={rotulo}>
      {opcoes.map(([k, l]) => (
        <button key={k} type="button" className="chip" aria-pressed={k === valor} onClick={() => onChange(k)}>
          {l}
        </button>
      ))}
    </div>
  );
}

/* ------------------------------- Formulários ------------------------------- */

export function Fld({ label, obrig, full, hint, hidden, children }: { label: React.ReactNode; obrig?: boolean; full?: boolean; hint?: React.ReactNode; hidden?: boolean; children: React.ReactNode }) {
  if (hidden) return null;
  return (
    <label className={`fld${full ? " full" : ""}`}>
      <span>
        {label}
        {obrig ? " *" : ""}
      </span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}

export function Sel<K extends string | number>({
  value,
  onChange,
  opcoes,
  vazio,
  grupos,
  disabled,
  id,
  ariaLabel,
  className,
}: {
  value: K | "" | null | undefined;
  onChange: (v: string) => void;
  opcoes?: readonly (readonly [K, string])[];
  grupos?: { grupo: string; itens: readonly (readonly [K, string])[] }[];
  vazio?: string;
  disabled?: boolean;
  id?: string;
  ariaLabel?: string;
  className?: string;
}) {
  return (
    <select id={id} className={className} aria-label={ariaLabel} value={value == null ? "" : String(value)} onChange={(e) => onChange(e.target.value)} disabled={disabled}>
      {vazio != null && <option value="">{vazio}</option>}
      {opcoes?.map(([k, l]) => (
        <option key={String(k)} value={String(k)}>
          {l}
        </option>
      ))}
      {grupos?.map((g) => (
        <optgroup key={g.grupo} label={g.grupo}>
          {g.itens.map(([k, l]) => (
            <option key={String(k)} value={String(k)}>
              {l}
            </option>
          ))}
        </optgroup>
      ))}
    </select>
  );
}

/** Mensagem de erro de uma exceção (ApiError, Error ou desconhecida). */
export function msgErro(e: unknown, padrao = "Não foi possível salvar. Verifique a conexão e tente de novo."): string {
  return e instanceof Error && e.message ? e.message : padrao;
}

/**
 * Formulário em folha: intro, campos, erro, ações e confirmação de exclusão,
 * no mesmo arranjo do protótipo. `onSalvar` grava; ao concluir, fecha a folha
 * e recarrega os dados (a não ser que `depois` assuma).
 */
export function FormSheet({
  intro,
  children,
  onSalvar,
  validar,
  okMsg = "Salvo",
  submit = "Salvar",
  onExcluir,
  excluirLabel = "Excluir",
  excluirConfirma = "Excluir de vez? Isso não pode ser desfeito.",
  depois,
  depoisExcluir,
}: {
  intro?: React.ReactNode;
  children: React.ReactNode;
  onSalvar: () => Promise<unknown>;
  validar?: () => string | null;
  okMsg?: string;
  submit?: string;
  onExcluir?: (() => Promise<unknown>) | null;
  excluirLabel?: string;
  excluirConfirma?: string;
  depois?: (resultado: unknown) => void;
  depoisExcluir?: () => void;
}) {
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [confirmando, setConfirmando] = useState(false);

  async function enviar(ev: React.FormEvent) {
    ev.preventDefault();
    const e = validar?.() ?? null;
    if (e) {
      setErro(e);
      return;
    }
    setErro(null);
    setOcupado(true);
    try {
      const r = await onSalvar();
      if (okMsg) showToast(okMsg);
      recarregar();
      if (depois) depois(r);
      else fechar();
    } catch (err) {
      setErro(msgErro(err));
    } finally {
      setOcupado(false);
    }
  }

  async function excluir() {
    if (!onExcluir) return;
    setOcupado(true);
    try {
      await onExcluir();
      showToast("Excluído");
      recarregar();
      if (depoisExcluir) depoisExcluir();
      else fechar();
    } catch (err) {
      setErro(msgErro(err, "Não foi possível excluir."));
      setConfirmando(false);
    } finally {
      setOcupado(false);
    }
  }

  return (
    <form className="form" noValidate onSubmit={enviar}>
      {intro && <p className="note" style={{ gridColumn: "1/-1" }}>{intro}</p>}
      {children}
      {erro && <p className="erro-form" role="alert">{erro}</p>}
      <div className="actions">
        {onExcluir && (
          <button type="button" className="btn danger left" onClick={() => setConfirmando(true)} disabled={ocupado}>
            {excluirLabel}
          </button>
        )}
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="submit" className="btn primary" disabled={ocupado}>
          {submit}
        </button>
      </div>
      {confirmando && (
        <div className="confirm">
          <p>{excluirConfirma}</p>
          <button type="button" className="btn sm" onClick={() => setConfirmando(false)}>
            Cancelar
          </button>
          <button type="button" className="btn sm danger solid" onClick={excluir} disabled={ocupado}>
            Excluir
          </button>
        </div>
      )}
    </form>
  );
}

/** Confirmação inline (fora de formulário). */
export function Confirma({ msg, onSim, onNao, rotulo = "Confirmar" }: { msg: React.ReactNode; onSim: () => void; onNao: () => void; rotulo?: string }) {
  return (
    <div className="confirm">
      <p>{msg}</p>
      <button type="button" className="btn sm" onClick={onNao}>
        Cancelar
      </button>
      <button type="button" className="btn sm danger solid" onClick={onSim}>
        {rotulo}
      </button>
    </div>
  );
}

/** "Copiar texto" para WhatsApp, com seleção manual quando o navegador não deixa copiar. */
export function CopiarTexto({ texto, nota = "Para colar no WhatsApp do fornecedor ou da cozinha.", rotulo = "Copiar texto" }: { texto: string; nota?: string; rotulo?: string }) {
  const { showToast } = useToast();
  const [mostrar, setMostrar] = useState(false);
  return (
    <>
      <div className="toolbar noprint">
        <button
          type="button"
          className="btn sm"
          onClick={() => {
            const fb = () => {
              setMostrar(true);
              showToast("Selecionei o texto: use Copiar do seu aparelho.");
            };
            try {
              navigator.clipboard.writeText(texto).then(() => showToast("Texto copiado"), fb);
            } catch {
              fb();
            }
          }}
        >
          {rotulo}
        </button>
        <span className="note">{nota}</span>
      </div>
      {mostrar && <textarea className="copybox" readOnly value={texto} aria-label="Texto para copiar" onFocus={(e) => e.currentTarget.select()} autoFocus />}
    </>
  );
}
