"use client";

import Link from "next/link";
import { useState } from "react";
import { Carregando, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { RANK_STATUS, STATUS_EVENTO, STATUS_ITEM_CARDAPIO, type StatusEvento, type StatusItemCardapio } from "@/lib/models/dominio";
import type { CardapioLinhaDTO, EventoDetalheResponseDTO } from "@/lib/models/evento";
import { detalheEvento, patchEvento, patchItemCardapio } from "@/lib/api/gestao/eventos";
import { brl, dec, fdate, numIn, pct } from "@/lib/utils/numero";
import { hoje, round2 } from "@/lib/utils/datas";
import { LancRow } from "../caixa/LancRow";
import { LancamentoForm, useAbrirLancamento } from "../caixa/LancamentoForm";
import { ContratoForm } from "./ContratoForm";
import { EventoForm } from "./EventoForm";
import { ExtraForm, ParcelaForm } from "./ParcelaExtraForms";
import { CardapioItemForm, CardapioMultiSheet, CopiarCardapioForm, ImportarCardapioSheet } from "./CardapioForms";
import { ClienteForm } from "../clientes/ClienteForm";
import { obterCliente } from "@/lib/api/gestao/clientes";
import { stLabel, tipoLabel } from "./status";
import { EquipeSecao, LogisticaSecao, ProjetoSecoes } from "./Projeto";

const ABAS_COZINHA = [
  ["producao", "Lista de produção"],
  ["insumos", "Insumos por prato"],
  ["compras", "Lista de compras"],
  ["cronograma", "Cronograma"],
  ["checklist", "Checklist de envio"],
] as const;

/** Ficha do evento: tudo do evento numa folha, como no protótipo. */
export function EventoSheet({ id }: { id: number }) {
  const { dados: det, erro } = useDados(() => detalheEvento(id), [id]);
  if (!det) return <Carregando erro={erro} />;
  return <EventoCorpo det={det} />;
}

function EventoCorpo({ det }: { det: EventoDetalheResponseDTO }) {
  const { abrir, recarregar, fechar } = useSheet();
  const { showToast } = useToast();
  const abrirLanc = useAbrirLancamento();
  const e = det.evento;
  const k = e.contrato;
  const t = hoje();

  async function mudar(fn: () => Promise<unknown>, ok: string) {
    try {
      await fn();
      showToast(ok);
      recarregar();
    } catch (err) {
      showToast(msgErro(err), "error");
    }
  }

  const prazoPill =
    det.prazoCardapio && RANK_STATUS[e.status] < RANK_STATUS.cardapio_fechado && e.status !== "perdido" && e.status !== "cancelado" && det.diasPrazo != null ? (
      <Pill tom={det.diasPrazo < 0 ? "bad" : det.diasPrazo <= 10 ? "warn" : ""}>{det.diasPrazo < 0 ? "passou" : det.diasPrazo === 0 ? "hoje" : `${det.diasPrazo} dias`}</Pill>
    ) : null;

  return (
    <>
      <div className="sec">
        <div className="toolbar between">
          <Pill>{tipoLabel(e.tipo)}</Pill>
          <Sel className="sel-inline" ariaLabel="Etapa do evento" value={e.status} opcoes={STATUS_EVENTO} onChange={(x) => mudar(() => patchEvento(e.id, { status: x as StatusEvento }), `Etapa: ${stLabel(x as StatusEvento)}`)} />
        </div>
        <h2>
          <span className="num" style={{ color: "var(--muted)" }}>
            {det.codigo}
          </span>{" "}
          · {e.nome}
        </h2>
      </div>
      <dl className="facts">
        <div>
          <dt>Data</dt>
          <dd>
            {fdate(e.dataEvento)}
            {e.horaInicio ? ` · ${e.horaInicio}` : ""}
          </dd>
        </div>
        <div>
          <dt>Local</dt>
          <dd>{[e.local, e.cidade].filter(Boolean).join(", ") || "—"}</dd>
        </div>
        <div>
          <dt>Cliente</dt>
          <dd>
            {det.cliente ? (
              <button type="button" className="btn sm ghost" style={{ padding: 0, fontWeight: 600 }} onClick={() => abrir("Editar cliente", <ClienteSheet id={det.cliente!.id} />)}>
                {det.cliente.nome}
              </button>
            ) : (
              "—"
            )}
          </dd>
        </div>
        <div>
          <dt>Contato</dt>
          <dd style={{ userSelect: "all" }}>{det.cliente?.contato || "—"}</dd>
        </div>
        <div>
          <dt>Convidados</dt>
          <dd className="num">{k ? `${k.convidadosContratados} contratados` : e.convidadosPrevistos ? `${e.convidadosPrevistos} previstos` : "—"}</dd>
        </div>
        <div>
          <dt>Prazo do cardápio</dt>
          <dd>
            {det.prazoCardapio ? fdate(det.prazoCardapio) : "—"} {prazoPill}
          </dd>
        </div>
      </dl>
      <ProjetoSecoes det={det} mudar={mudar} />

      <div className="sec">
        <div className="sec-h">
          <h3>Contrato</h3>
          {k && (
            <button type="button" className="btn sm" onClick={() => abrir("Editar contrato", <ContratoForm det={det} />)}>
              Editar
            </button>
          )}
        </div>
        {k ? (
          <dl className="facts">
            <div>
              <dt>Número</dt>
              <dd>{k.numero || "—"}</dd>
            </div>
            <div>
              <dt>Assinado em</dt>
              <dd>{fdate(k.dataAssinatura)}</dd>
            </div>
            <div>
              <dt>Valor total</dt>
              <dd className="num">{brl(k.valorTotal)}</dd>
            </div>
            <div>
              <dt>Por convidado</dt>
              <dd className="num">{k.valorPorConvidado ? brl(k.valorPorConvidado) : "—"}</dd>
            </div>
            <div>
              <dt>Convidado extra</dt>
              <dd className="num">{k.valorConvidadoExtra ? brl(k.valorConvidadoExtra) : "—"}</dd>
            </div>
            <div>
              <dt>Recebido</dt>
              <dd className="num">
                {brl(det.recebido)} de {brl(det.totalParcelas)}
              </dd>
            </div>
          </dl>
        ) : (
          <div className="box">
            <Vazio titulo="Sem contrato">
              Registre o contrato para gerar as parcelas e as receitas previstas.
              <div style={{ marginTop: 10 }}>
                <button type="button" className="btn primary sm" onClick={() => abrir("Registrar contrato", <ContratoForm det={det} />)}>
                  Registrar contrato
                </button>
              </div>
            </Vazio>
          </div>
        )}
      </div>

      {k && (
        <div className="sec">
          <div className="sec-h">
            <h3>Parcelas</h3>
          </div>
          <div className="box list">
            {det.parcelas.length ? (
              det.parcelas.map((p) => {
                const venc = !p.pagoEm && p.vencimento < t;
                return (
                  <button key={p.id} type="button" className="row" onClick={() => abrir(`Parcela ${p.numero} · ${e.nome}`, <ParcelaForm parcela={p} conta="banco" />)}>
                    <span className="grow">
                      <span className="t">{p.numero}ª parcela</span>
                      <span className="m">
                        {p.pagoEm
                          ? `recebida em ${fdate(p.pagoEm)}`
                          : `vence ${fdate(p.vencimento)}${p.recebido > 0 ? ` · recebido ${brl(p.recebido)}, falta ${brl(p.valor - p.recebido)}` : ""}`}
                      </span>
                    </span>
                    {p.pagoEm ? (
                      <Pill tom="good">recebida</Pill>
                    ) : p.recebido > 0 ? (
                      <Pill tom={venc ? "bad" : "warn"}>parcial</Pill>
                    ) : venc ? (
                      <Pill tom="bad">vencida</Pill>
                    ) : (
                      <Pill>em aberto</Pill>
                    )}
                    <span className="amt">{brl(p.valor)}</span>
                  </button>
                );
              })
            ) : (
              <Vazio>Nenhuma parcela.</Vazio>
            )}
          </div>
        </div>
      )}

      <CardapioSecao det={det} mudar={mudar} />
      <EquipeSecao det={det} />
      <LogisticaSecao det={det} />

      <div className="sec">
        <div className="sec-h">
          <h3>Extras</h3>
          <button type="button" className="btn sm" onClick={() => abrir("Novo extra", <ExtraForm evento={e} />)}>
            Adicionar
          </button>
        </div>
        <div className="box list">
          {det.extras.length ? (
            det.extras.map((x) => (
              <button key={x.id} type="button" className="row" onClick={() => abrir("Editar extra", <ExtraForm evento={e} extra={x} />)}>
                <span className="grow">
                  <span className="t">{x.descricao}</span>
                  <span className="m">
                    {dec(x.quantidade)} × {brl(x.valorUnitario)}
                  </span>
                </span>
                <span className="amt">{brl(round2(x.quantidade * x.valorUnitario))}</span>
              </button>
            ))
          ) : (
            <Vazio>Nenhum extra lançado.</Vazio>
          )}
        </div>
      </div>

      <div className="sec">
        <div className="sec-h">
          <h3>Custos e outros lançamentos</h3>
          <button type="button" className="btn sm" onClick={() => abrir("Novo lançamento", <LancamentoForm preset={{ eventoId: e.id, dataCompetencia: e.dataEvento || hoje() }} />)}>
            Lançar custo
          </button>
        </div>
        <div className="box list">
          {det.outros.length ? (
            det.outros.map((l) => (
              <LancRow key={l.id} l={l} onAbrir={() => abrirLanc(l)} />
            ))
          ) : (
            <Vazio>Lance compras de A&amp;B, equipe, frete e locação aqui para ver a margem real do evento.</Vazio>
          )}
        </div>
      </div>

      <div className="sec">
        <h3>Observações</h3>
        {e.observacoes ? <p style={{ margin: 0, whiteSpace: "pre-wrap" }}>{e.observacoes}</p> : <p className="note">Nenhuma observação registrada. Use Editar evento para incluir.</p>}
      </div>
      <div className="toolbar">
        <button type="button" className="btn" onClick={() => abrir("Editar evento", <EventoForm evento={e} travado={det.travado} />)}>
          Editar evento
        </button>
        <button type="button" className="btn ghost" onClick={() => fechar(true)}>
          Fechar
        </button>
      </div>
    </>
  );
}

function CardapioSecao({ det, mudar }: { det: EventoDetalheResponseDTO; mudar: (fn: () => Promise<unknown>, ok: string) => Promise<void> }) {
  const { abrir, fechar } = useSheet();
  const { showToast } = useToast();
  const e = det.evento;
  const nd = det.cardapio;
  const fechado = nd.fechado;
  const [margem, setMargem] = useState(dec(nd.margem));

  function salvarMargem() {
    const n = numIn(margem);
    if (n < 0 || n > 100) return showToast("Use uma margem entre 0 e 100%.", "error");
    if (Math.abs(n - nd.margem) < 1e-9) return;
    mudar(() => patchEvento(e.id, { margem: n }), `Margem de ${dec(n)}%`);
  }

  return (
    <div className="sec">
      <div className="sec-h">
        <h3>Cardápio e produção</h3>
        {fechado ? <Pill tom="good">fechado em {fdate(e.cardapioFechadoEm)}</Pill> : <Pill>em montagem</Pill>}
      </div>
      <div className="prev">
        <div>
          <span className="k">Previsto para</span>
          <b className="num">{nd.N}</b>{" "}
          <span className="m">
            pessoas{nd.base ? ` (${nd.base} + ${dec(nd.margem)}%)` : ""}
          </span>
        </div>
        <label className="fator">
          <span className="m">Margem</span>
          <input className="inline-num num" inputMode="decimal" value={margem} onChange={(x) => setMargem(x.target.value)} onBlur={salvarMargem} disabled={fechado} aria-label="Margem de produção" />
          <span className="m">%</span>
        </label>
      </div>
      {!nd.base && <p className="note warn-t">Informe os convidados no evento ou no contrato para calcular as quantidades.</p>}
      <div className="box list">
        {!nd.linhas.length && (
          <Vazio titulo="Nenhum prato no cardápio">Importe o cardápio recebido do cliente ou adicione os pratos um a um. Quantidades, compras, produção e estoque saem das fichas técnicas.</Vazio>
        )}
        {nd.linhas.map((x, i) => {
          const cab = i === 0 || nd.linhas[i - 1]!.categoria !== x.categoria ? <div className="monthhead">{x.categoriaLabel}</div> : null;
          return (
            <div key={x.item.id}>
              {cab}
              <LinhaCardapio x={x} fechado={fechado} onAbrir={() => abrir("Prato do cardápio", <CardapioItemForm evento={e} linha={x} />)} mudar={mudar} />
            </div>
          );
        })}
      </div>
      <div className="toolbar">
        {!fechado && (
          <>
            <button type="button" className="btn sm" onClick={() => abrir("Importar cardápio", <ImportarCardapioSheet evento={e} />)}>
              Importar cardápio
            </button>
            <button type="button" className="btn sm primary" onClick={() => abrir("Adicionar pratos ao cardápio", <CardapioMultiSheet evento={e} jaNoCardapio={nd.linhas.map((l) => l.item.preparacaoId)} />, { larga: true })}>
              Adicionar pratos
            </button>
            {!nd.linhas.length && (
              <button type="button" className="btn sm" onClick={() => abrir("Copiar cardápio", <CopiarCardapioForm evento={e} />)}>
                Copiar de outro evento
              </button>
            )}
          </>
        )}
        {nd.linhas.length > 0 &&
          (fechado ? (
            <button type="button" className="btn sm ghost" onClick={() => mudar(() => patchEvento(e.id, { cardapioFechado: false }), "Cardápio reaberto")}>
              Reabrir cardápio
            </button>
          ) : (
            <button type="button" className="btn sm primary" onClick={() => mudar(() => patchEvento(e.id, { cardapioFechado: true }), "Cardápio fechado")}>
              Fechar cardápio
            </button>
          ))}
      </div>
      {nd.linhas.length > 0 && (
        <>
          <div className="toolbar">
            <span className="note" style={{ flexBasis: "100%" }}>
              Produção, compras, cronograma e conferência ficam na área Cozinha, que a chef acessa sem ver valores.
            </span>
            {ABAS_COZINHA.map(([aba, l]) => (
              <Link key={aba} className="btn sm" href={`/cozinha?evento=${e.id}&aba=${aba}`} onClick={() => fechar(true)}>
                {l}
              </Link>
            ))}
          </div>
          <div className="summary">
            <span>
              Custo de A&amp;B previsto pelas fichas: <b className="num">{brl(nd.custo)}</b>
              {nd.custoPorPessoa != null && (
                <>
                  {" "}
                  · <span className="num">{brl(nd.custoPorPessoa)}</span> por pessoa
                </>
              )}
              {nd.pctContrato != null && ` · ${pct(nd.pctContrato)} do contrato`}
            </span>
            {nd.nDefinir > 0 && (
              <span className="warn-t">
                {nd.nDefinir} prato{nd.nDefinir > 1 ? "s" : ""} a definir ({brl(nd.custoDef)}): ficam fora da reserva de estoque até virar Sim.
              </span>
            )}
            {nd.abLancado > 0 && (
              <span>
                A&amp;B já lançado no evento: <b className="num">{brl(nd.abLancado)}</b>
                {nd.custo ? ` (${nd.abLancado > nd.custo ? "+" : ""}${pct((nd.abLancado - nd.custo) / nd.custo)} sobre o previsto)` : ""}
              </span>
            )}
            {nd.semFicha.length > 0 && <span className="warn-t">Sem ficha técnica: {nd.semFicha.join(", ")}.</span>}
            {nd.faltandoPreco && <span className="warn-t">Há insumos sem preço; o custo previsto está incompleto.</span>}
          </div>
        </>
      )}
    </div>
  );
}

function LinhaCardapio({ x, fechado, onAbrir, mudar }: { x: CardapioLinhaDTO; fechado: boolean; onAbrir: () => void; mudar: (fn: () => Promise<unknown>, ok: string) => Promise<void> }) {
  const { showToast } = useToast();
  const [porcao, setPorcao] = useState(dec(x.item.porcao));
  const fs = x.fichaStatus;
  function salvarPorcao() {
    const n = numIn(porcao);
    if (!(n > 0)) {
      showToast("O fator por convidado precisa ser maior que zero.", "error");
      setPorcao(dec(x.item.porcao));
      return;
    }
    if (Math.abs(n - x.item.porcao) > 1e-9) mudar(() => patchItemCardapio(x.item.id, { porcao: n }), "Fator atualizado");
  }
  const st = (v: StatusItemCardapio) => mudar(() => patchItemCardapio(x.item.id, { status: v }), v === "sim" ? "Prato confirmado" : v === "definir" ? "Prato a definir" : "Prato fora do cardápio");
  return (
    <div className={`row cardrow${x.status === "nao" ? " off" : ""}`}>
      <button type="button" className="grow linkish" onClick={onAbrir}>
        <span className="t wrap">{x.preparacaoNome}</span>
        <span className="m wrap">
          {[x.pracaEfetiva, x.item.responsavel ? `resp. ${x.item.responsavel}` : null, x.aProduzir].filter(Boolean).join(" · ")}
          {fs !== "Completa" && <span className={fs === "Incompleta" ? "neg" : "warn-t"}> · ficha {fs.toLowerCase()}</span>}
        </span>
      </button>
      <span className="seg" role="group" aria-label="No cardápio">
        {STATUS_ITEM_CARDAPIO.map(([k, l]) => (
          <button key={k} type="button" data-v={k} className={x.status === k ? "on" : ""} disabled={fechado} onClick={() => x.status !== k && st(k)}>
            {l}
          </button>
        ))}
      </span>
      <label className="fator">
        <input className="inline-num num" inputMode="decimal" value={porcao} onChange={(e) => setPorcao(e.target.value)} onBlur={salvarPorcao} disabled={fechado} aria-label={`Fator por convidado de ${x.preparacaoNome}`} />
        <span className="m">{x.fatorUnid}/conv.</span>
      </label>
      <span className="amt">{x.custo ? brl(x.custo) : x.custoDef ? <span className="warn-t">{brl(x.custoDef)}</span> : "—"}</span>
    </div>
  );
}

/** Cadastro do cliente aberto a partir da ficha do evento (protótipo: botão com o nome do cliente). */
function ClienteSheet({ id }: { id: number }) {
  const { dados: c, erro } = useDados(() => obterCliente(id), [id]);
  if (!c) return <Carregando erro={erro} />;
  return <ClienteForm cliente={c} temEventos={c.totalEventos > 0} />;
}
