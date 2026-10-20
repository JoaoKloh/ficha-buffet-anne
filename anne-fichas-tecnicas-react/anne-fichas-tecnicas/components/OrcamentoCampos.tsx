"use client";

import { useId, useState } from "react";
import type { ItemCusto, ItemQtdValor } from "@/lib/models/orcamento";
import { fmtCurrency, fmtCurrencySigned } from "@/lib/utils/format";
import {
  removerEm,
  substituirEm,
  toNum,
  totalItemCusto,
  totalItemQtd,
  totalItensCusto,
  totalItensQtd,
} from "@/lib/utils/orcamento";

// Peças reutilizadas por todas as abas do Orçamento.

type NumInputProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type"> & {
  value: number;
  onChange: (value: number) => void;
};

/**
 * Input numérico que guarda o texto digitado: apagar o campo ou digitar "1,"
 * não é reescrito para "0"/"1" no meio da digitação. Se o valor mudar por fora
 * (ex.: quantidade "auto" da equipe ao alterar os convidados), o texto acompanha.
 */
export function NumInput({ value, onChange, ...rest }: NumInputProps) {
  const [texto, setTexto] = useState(String(value));
  const [anterior, setAnterior] = useState(value);
  if (value !== anterior) {
    setAnterior(value);
    if (toNum(texto) !== value) setTexto(String(value));
  }

  return (
    <input
      type="number"
      inputMode="decimal"
      min="0"
      {...rest}
      value={texto}
      onChange={(e) => {
        setTexto(e.target.value);
        onChange(toNum(e.target.value));
      }}
    />
  );
}

/** Rótulo + campo no padrão .field-block dos formulários da aplicação. */
export function Campo({
  label,
  children,
}: {
  label: string;
  children: (id: string) => React.ReactNode;
}) {
  const id = useId();
  return (
    <div className="field-block">
      <label htmlFor={id}>{label}</label>
      {children(id)}
    </div>
  );
}

export function OrcCard({
  titulo,
  dica,
  children,
}: {
  titulo: string;
  dica?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="fin-panel">
      <div className="orc-card-head">
        <h2 className="section-h">{titulo}</h2>
        {dica && <span className="fin-muted">{dica}</span>}
      </div>
      {children}
    </section>
  );
}

/** Indicador (rótulo + valor). `sinal` pinta o valor de verde/vermelho conforme o sinal. */
export function Stat({
  rotulo,
  valor,
  sinal,
  destaque = false,
}: {
  rotulo: string;
  valor: string;
  sinal?: number;
  destaque?: boolean;
}) {
  const tom = sinal === undefined ? "" : sinal < 0 ? "neg" : "pos";
  return (
    <div className="orc-stat">
      <div className="orc-stat-label">{rotulo}</div>
      <div className={`orc-stat-valor ${destaque ? "destaque" : ""} ${tom}`}>{valor}</div>
    </div>
  );
}

export function Acordeao({
  titulo,
  total,
  children,
}: {
  titulo: string;
  total: number;
  children: React.ReactNode;
}) {
  // Abre já expandido o que tem custo — o restante é catálogo de consulta.
  const [aberto, setAberto] = useState(total > 0);
  return (
    <details className="orc-acc" open={aberto} onToggle={(e) => setAberto(e.currentTarget.open)}>
      <summary>
        <span className="orc-acc-titulo">{titulo}</span>
        <span className="orc-acc-total">{fmtCurrency(total)}</span>
        <span className="orc-acc-seta" aria-hidden="true">
          ▸
        </span>
      </summary>
      <div className="orc-acc-body">{children}</div>
    </details>
  );
}

export function BotaoRemover({ rotulo, onClick }: { rotulo: string; onClick: () => void }) {
  return (
    <button type="button" className="row-remove" aria-label={rotulo} title={rotulo} onClick={onClick}>
      ✕
    </button>
  );
}

export function BotaoAdicionar({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <div className="orc-add">
      <button type="button" className="btn ghost small" onClick={onClick}>
        + {children}
      </button>
    </div>
  );
}

/** Tabela de itens "incluir × quantidade × valor" (bebidas, gelo, descartáveis, louças, pacote). */
export function TabelaItens({
  itens,
  onChange,
  rotuloNome = "Item",
}: {
  itens: ItemQtdValor[];
  onChange: (itens: ItemQtdValor[]) => void;
  rotuloNome?: string;
}) {
  function atualizar(i: number, patch: Partial<ItemQtdValor>) {
    const atual = itens[i];
    if (atual) onChange(substituirEm(itens, i, { ...atual, ...patch }));
  }

  return (
    <>
      <div className="orc-table-wrap">
        <table className="orc-table">
          <thead>
            <tr>
              <th aria-label="Incluir" />
              <th>{rotuloNome}</th>
              <th className="num">Qtd.</th>
              <th className="num">Valor unit.</th>
              <th className="num">Total</th>
              <th aria-label="Ações" />
            </tr>
          </thead>
          <tbody>
            {itens.map((it, i) => (
              <tr key={i} className={it.incluir ? "" : "off"}>
                <td>
                  <input
                    type="checkbox"
                    className="orc-chk"
                    checked={it.incluir}
                    aria-label={`Incluir ${it.nome || "item"}`}
                    onChange={(e) => atualizar(i, { incluir: e.target.checked })}
                  />
                </td>
                <td>
                  <input
                    type="text"
                    className="nome"
                    value={it.nome}
                    placeholder="Nome do item"
                    aria-label={rotuloNome}
                    onChange={(e) => atualizar(i, { nome: e.target.value })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="qtd"
                    step="1"
                    value={it.qtd}
                    aria-label={`Quantidade de ${it.nome || "item"}`}
                    onChange={(qtd) => atualizar(i, { qtd })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="valor"
                    step="0.01"
                    value={it.valorUnit}
                    aria-label={`Valor unitário de ${it.nome || "item"}`}
                    onChange={(valorUnit) => atualizar(i, { valorUnit })}
                  />
                </td>
                <td className="num total">{fmtCurrency(totalItemQtd(it))}</td>
                <td>
                  <BotaoRemover
                    rotulo={`Remover ${it.nome || "item"}`}
                    onClick={() => onChange(removerEm(itens, i))}
                  />
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={4}>Subtotal</td>
              <td className="num">{fmtCurrency(totalItensQtd(itens))}</td>
              <td />
            </tr>
          </tfoot>
        </table>
      </div>
      <BotaoAdicionar onClick={() => onChange([...itens, { nome: "", qtd: 0, valorUnit: 0, incluir: true }])}>
        Adicionar item
      </BotaoAdicionar>
    </>
  );
}

/** Tabela de custos sempre incluídos (verba extra, degustação). */
export function TabelaCustos({
  itens,
  onChange,
}: {
  itens: ItemCusto[];
  onChange: (itens: ItemCusto[]) => void;
}) {
  function atualizar(i: number, patch: Partial<ItemCusto>) {
    const atual = itens[i];
    if (atual) onChange(substituirEm(itens, i, { ...atual, ...patch }));
  }

  return (
    <>
      <div className="orc-table-wrap">
        <table className="orc-table">
          <thead>
            <tr>
              <th>Descrição</th>
              <th className="num">Qtd.</th>
              <th className="num">Valor unit.</th>
              <th className="num">Total</th>
              <th aria-label="Ações" />
            </tr>
          </thead>
          <tbody>
            {itens.map((it, i) => (
              <tr key={i}>
                <td>
                  <input
                    type="text"
                    className="nome"
                    value={it.nome}
                    placeholder="Descrição"
                    aria-label="Descrição"
                    onChange={(e) => atualizar(i, { nome: e.target.value })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="qtd"
                    step="1"
                    value={it.qtd}
                    aria-label={`Quantidade de ${it.nome || "item"}`}
                    onChange={(qtd) => atualizar(i, { qtd })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="valor"
                    step="0.01"
                    value={it.valorUnit}
                    aria-label={`Valor unitário de ${it.nome || "item"}`}
                    onChange={(valorUnit) => atualizar(i, { valorUnit })}
                  />
                </td>
                <td className="num total">{fmtCurrency(totalItemCusto(it))}</td>
                <td>
                  <BotaoRemover
                    rotulo={`Remover ${it.nome || "item"}`}
                    onClick={() => onChange(removerEm(itens, i))}
                  />
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={3}>Subtotal</td>
              <td className="num">{fmtCurrency(totalItensCusto(itens))}</td>
              <td />
            </tr>
          </tfoot>
        </table>
      </div>
      <BotaoAdicionar onClick={() => onChange([...itens, { nome: "", qtd: 1, valorUnit: 0 }])}>
        Adicionar item
      </BotaoAdicionar>
    </>
  );
}

/** Célula de valor com sinal (saldo/contribuição): verde se ≥ 0, vermelho se negativo. */
export function ValorComSinal({ valor }: { valor: number }) {
  return <span className={valor < 0 ? "neg" : "pos"}>{fmtCurrencySigned(valor)}</span>;
}
