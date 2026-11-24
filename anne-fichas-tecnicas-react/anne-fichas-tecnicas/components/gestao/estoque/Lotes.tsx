"use client";

import { useState } from "react";
import { Carregando, CopiarTexto, Datebox, FormSheet, Fld, Pill, Sel, Vazio } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { DESTINOS_LOTE, MOTIVOS_DESCARTE_LOTE, USOS_LOTE, rotulo, type DestinoLote } from "@/lib/models/dominio";
import type { LoteListaItemDTO, MovimentarLoteRequestDTO } from "@/lib/models/estoque";
import { atualizarLote, criarLote, excluirLote, movimentarLote, obterLote, opcoesEstoque } from "@/lib/api/gestao/estoque";
import { brl, dec, fdate, fmtN, numIn } from "@/lib/utils/numero";
import { addDays, hoje, r3 } from "@/lib/utils/datas";

const unidRend = (u: string, n: number) => (u === "porções" ? (n === 1 ? "porção" : "porções") : u === "un" ? "un" : u);
const PARA: Record<string, string> = { restaurante: "Serviço do restaurante", evento: "Evento", outra: "Outra preparação", vencido: "Venceu", qualidade: "Qualidade", acidente: "Acidente", sobra: "Sobra do serviço" };

export function LoteRow({ l, onClick }: { l: LoteListaItemDTO; onClick: () => void }) {
  return (
    <button type="button" className="row" style={l.saldo > 0 ? undefined : { opacity: 0.6 }} onClick={onClick}>
      <Datebox iso={l.validade} />
      <span className="grow">
        <span className="t">{l.preparacaoNome}</span>
        <span className="m">{[l.codigo, `feito ${fdate(l.produzidoEm)}`, l.destino === "evento" && l.eventoNome ? l.eventoNome : rotulo(DESTINOS_LOTE, l.destino), l.responsavel].filter(Boolean).join(" · ")}</span>
      </span>
      <Pill tom={l.situacao[0]}>{l.situacao[1]}</Pill>
      <span className="amt">
        {fmtN(r3(l.saldo))} {unidRend(l.unidade, l.saldo)}
      </span>
    </button>
  );
}

/** Novo lote, ou edição (com uso registrado: só validade, responsável e observações). */
export function LoteForm({ lote }: { lote?: LoteListaItemDTO }) {
  const { dados: op, erro } = useDados(() => opcoesEstoque("lote"));
  const editavel = !lote || !lote.historico.length;
  const [v, setV] = useState({
    preparacaoId: lote ? String(lote.preparacaoId) : "",
    qtd: lote ? dec(lote.qtd) : "",
    produzidoEm: lote?.produzidoEm ?? hoje(),
    validade: lote?.validade ?? "",
    destino: (lote?.destino ?? "restaurante") as DestinoLote,
    eventoId: lote?.eventoId ? String(lote.eventoId) : "",
    responsavel: lote?.responsavel ?? "",
    obs: lote?.obs ?? "",
  });
  if (!op) return <Carregando erro={erro} />;
  if (!lote && !op.preparacoes.length) return <p className="note">Cadastre as fichas técnicas primeiro, na aba Fichas.</p>;
  const info = v.preparacaoId ? op.preparacoesInfo[Number(v.preparacaoId)] : undefined;
  const recalc = (prepId: string, produzidoEm: string) => {
    const p = op.preparacoesInfo[Number(prepId)];
    return p && produzidoEm ? addDays(produzidoEm, p.validadeDias) : v.validade;
  };
  const dtoCompleto = () => ({
    preparacaoId: Number(v.preparacaoId), qtd: numIn(v.qtd), produzidoEm: v.produzidoEm, validade: v.validade, destino: v.destino,
    eventoId: v.destino === "evento" && v.eventoId ? Number(v.eventoId) : null, responsavel: v.responsavel, obs: v.obs,
  });
  return (
    <FormSheet
      intro={editavel ? undefined : "Este lote já teve uso ou descarte; só dá para mudar validade, responsável e observações."}
      okMsg={lote ? "Lote salvo" : "Lote registrado"}
      validar={() =>
        editavel && !v.preparacaoId
          ? "Escolha a preparação."
          : editavel && !(numIn(v.qtd) > 0)
            ? "Informe a quantidade produzida."
            : editavel && !v.produzidoEm
              ? "Informe a data de produção."
              : v.validade && editavel && v.validade < v.produzidoEm
                ? "A validade não pode ser antes da produção."
                : editavel && v.destino === "evento" && !v.eventoId
                  ? "Escolha o evento."
                  : null
      }
      onSalvar={() => (!lote ? criarLote(dtoCompleto()) : editavel ? atualizarLote(lote.id, dtoCompleto()) : atualizarLote(lote.id, { validade: v.validade, responsavel: v.responsavel, obs: v.obs }))}
      onExcluir={lote && editavel ? () => excluirLote(lote.id) : null}
      excluirLabel="Excluir lote"
      excluirConfirma="Excluir este lote? A baixa dos insumos que ele fez também sai."
    >
      <Fld label="Preparação" full hidden={!editavel}>
        <Sel value={v.preparacaoId} onChange={(x) => setV((s) => ({ ...s, preparacaoId: x, validade: recalc(x, s.produzidoEm) }))} grupos={op.preparacoes.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))} vazio="Escolha…" />
      </Fld>
      <Fld label="Quantidade produzida" hidden={!editavel} hint={info ? `Em ${unidRend(info.unidade, 2)}. Uma receita rende ${fmtN(info.rendimento)} ${unidRend(info.unidade, info.rendimento)}.` : "Escolha a preparação."}>
        <input inputMode="decimal" value={v.qtd} onChange={(e) => setV({ ...v, qtd: e.target.value })} />
      </Fld>
      <Fld label="Produzido em" hidden={!editavel}>
        <input type="date" value={v.produzidoEm} onChange={(e) => setV((s) => ({ ...s, produzidoEm: e.target.value, validade: s.preparacaoId ? recalc(s.preparacaoId, e.target.value) : s.validade }))} />
      </Fld>
      <Fld label="Válido até" hint={info ? `Padrão da ficha: ${info.validadeDias} dia${info.validadeDias === 1 ? "" : "s"} (${info.conservacao.toLowerCase()}).` : "Sugerida pela ficha ao escolher a preparação."}>
        <input type="date" value={v.validade} onChange={(e) => setV({ ...v, validade: e.target.value })} />
      </Fld>
      <Fld label="Para" hidden={!editavel}>
        <Sel value={v.destino} onChange={(x) => setV({ ...v, destino: x as DestinoLote })} opcoes={DESTINOS_LOTE} />
      </Fld>
      <Fld label="Evento" hidden={!editavel || v.destino !== "evento"}>
        <Sel value={v.eventoId} onChange={(x) => setV({ ...v, eventoId: x })} opcoes={op.eventosLote.map(([id, n]) => [String(id), n] as const)} vazio="Escolha…" />
      </Fld>
      <Fld label="Quem produziu">
        <input value={v.responsavel} onChange={(e) => setV({ ...v, responsavel: e.target.value })} placeholder="Nome" />
      </Fld>
      <Fld label="Observações" full>
        <textarea value={v.obs} onChange={(e) => setV({ ...v, obs: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}

/** Usar ou descartar parte do lote. No uso em evento, diz qual: a ordem e o checklist dele deixam de baixar essa quantidade. */
export function LoteMovForm({ lote, tipo }: { lote: LoteListaItemDTO; tipo: "uso" | "descarte" }) {
  const s = lote.saldo;
  const { dados: op, erro } = useDados(() => opcoesEstoque("lote"));
  const [v, setV] = useState({
    qtd: tipo === "descarte" ? dec(r3(s)) : "", data: hoje(), para: tipo === "uso" ? (lote.destino === "evento" ? "evento" : "restaurante") : lote.situacao[1] === "Vencido" ? "vencido" : "qualidade",
    eventoId: lote.eventoId != null ? String(lote.eventoId) : "", obs: "",
  });
  const emEvento = tipo === "uso" && v.para === "evento";
  return (
    <FormSheet
      intro={
        <>
          Saldo do lote:{" "}
          <b>
            {fmtN(r3(s))} {unidRend(lote.unidade, s)}
          </b>
          .
        </>
      }
      okMsg={tipo === "uso" ? "Uso registrado" : "Descarte registrado"}
      validar={() => {
        const q = numIn(v.qtd);
        return !(q > 0) ? "Informe a quantidade." : q > s + 1e-9 ? `Maior que o saldo do lote (${fmtN(r3(s))}).` : emEvento && !v.eventoId ? "Escolha o evento." : null;
      }}
      onSalvar={() =>
        movimentarLote(lote.id, { tipo, qtd: numIn(v.qtd), data: v.data || hoje(), para: v.para as MovimentarLoteRequestDTO["para"], eventoId: emEvento ? Number(v.eventoId) : null, obs: v.obs })
      }
    >
      <Fld label={`Quantidade (${unidRend(lote.unidade, 2)})`} obrig>
        <input inputMode="decimal" value={v.qtd} onChange={(e) => setV({ ...v, qtd: e.target.value })} />
      </Fld>
      <Fld label="Data">
        <input type="date" value={v.data} onChange={(e) => setV({ ...v, data: e.target.value })} />
      </Fld>
      <Fld label={tipo === "uso" ? "Usado para" : "Motivo"} full>
        <Sel value={v.para} onChange={(x) => setV({ ...v, para: x })} opcoes={tipo === "uso" ? USOS_LOTE : MOTIVOS_DESCARTE_LOTE} />
      </Fld>
      <Fld
        label="Qual evento"
        full
        hidden={!emEvento}
        hint={lote.baixaInsumos ? "A ordem e o checklist do evento deixam de baixar os insumos desta quantidade, para não baixar duas vezes." : "Lote feito para o evento: a baixa dos insumos já é do evento."}
      >
        {op ? (
          <Sel value={v.eventoId} onChange={(x) => setV({ ...v, eventoId: x })} opcoes={op.eventosLote.map(([id, n]) => [String(id), n] as const)} vazio="Escolha…" />
        ) : (
          <Carregando erro={erro} />
        )}
      </Fld>
      <Fld label="Observação" full>
        <input value={v.obs} onChange={(e) => setV({ ...v, obs: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}

/** Ficha do lote: saldo, etiqueta para copiar, uso e descarte. */
export function LoteSheet({ id }: { id: number }) {
  const { dados: l, erro } = useDados(() => obterLote(id), [id]);
  const { abrir } = useSheet();
  if (!l) return <Carregando erro={erro} />;
  const u = l.unidade;
  const etiqueta = [l.preparacaoNome, `Lote ${l.codigo}`, `Produzido: ${fdate(l.produzidoEm)}`, `Validade: ${fdate(l.validade)}`, l.conservacao, l.responsavel ? `Resp.: ${l.responsavel}` : ""].filter(Boolean).join("\n");
  return (
    <>
      <div className="sec">
        <div className="toolbar between">
          <Pill>{l.codigo || "Lote"}</Pill>
          <Pill tom={l.situacao[0]}>{l.situacao[1]}</Pill>
        </div>
        <h2>{l.preparacaoNome}</h2>
      </div>
      <dl className="facts">
        <div>
          <dt>Saldo</dt>
          <dd className="num">
            {fmtN(r3(l.saldo))} {unidRend(u, l.saldo)}
          </dd>
        </div>
        <div>
          <dt>Produzido</dt>
          <dd className="num">
            {fmtN(r3(l.qtd))} {unidRend(u, l.qtd)}
          </dd>
        </div>
        <div>
          <dt>Feito em</dt>
          <dd>{fdate(l.produzidoEm)}</dd>
        </div>
        <div>
          <dt>Válido até</dt>
          <dd>{fdate(l.validade)}</dd>
        </div>
        <div>
          <dt>Para</dt>
          <dd>{l.destino === "evento" && l.eventoNome ? l.eventoNome : rotulo(DESTINOS_LOTE, l.destino)}</dd>
        </div>
        <div>
          <dt>Conservação</dt>
          <dd>{l.conservacao}</dd>
        </div>
        <div>
          <dt>Quem produziu</dt>
          <dd>{l.responsavel || "—"}</dd>
        </div>
        <div>
          <dt>Valor do saldo</dt>
          <dd className="num">{brl(l.valor)}</dd>
        </div>
      </dl>
      <div className="toolbar">
        {l.saldo > 0 && (
          <>
            <button type="button" className="btn sm primary" onClick={() => abrir(`Usar do lote ${l.codigo}`, <LoteMovForm lote={l} tipo="uso" />)}>
              Usar
            </button>
            <button type="button" className="btn sm" onClick={() => abrir(`Descartar do lote ${l.codigo}`, <LoteMovForm lote={l} tipo="descarte" />)}>
              Descartar
            </button>
          </>
        )}
        <button type="button" className="btn sm ghost" onClick={() => abrir(`Lote ${l.codigo}`, <LoteForm lote={l} />)}>
          Editar
        </button>
      </div>
      <div className="sec">
        <h3>Etiqueta</h3>
        <div className="box pad" style={{ fontSize: 15, lineHeight: 1.45 }}>
          <b style={{ fontSize: 17 }}>{l.preparacaoNome}</b>
          <br />
          Lote <b className="num">{l.codigo || "—"}</b> · {fmtN(r3(l.qtd))} {unidRend(u, l.qtd)}
          <br />
          Produzido: <b>{fdate(l.produzidoEm)}</b> · Validade: <b>{fdate(l.validade)}</b>
          <br />
          {l.conservacao}
          {l.responsavel ? ` · Resp.: ${l.responsavel}` : ""}
        </div>
        <CopiarTexto texto={etiqueta} />
      </div>
      {l.obs && <p className="note">{l.obs}</p>}
      <div className="sec">
        <h3>Uso e descarte</h3>
        <div className="box list">
          {l.historico.length ? (
            [...l.historico].reverse().map((x, i) => (
              <div key={i} className="row static">
                <Datebox iso={x.data} />
                <span className="grow">
                  <span className="t">{x.tipo === "uso" ? "Uso" : "Descarte"}</span>
                  <span className="m">{[PARA[x.para] ?? x.para, x.obs].filter(Boolean).join(" · ")}</span>
                </span>
                <span className={`amt${x.tipo === "descarte" ? " neg" : ""}`}>− {fmtN(x.qtd)}</span>
              </div>
            ))
          ) : (
            <Vazio>Nada usado ainda.</Vazio>
          )}
        </div>
      </div>
      {l.baixaInsumos && <p className="note">A produção deste lote deu baixa nos insumos da ficha (veja Estoque › Movimentos).</p>}
    </>
  );
}
