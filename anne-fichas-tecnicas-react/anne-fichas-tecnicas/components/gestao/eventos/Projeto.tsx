"use client";

import { useState } from "react";
import { FormSheet, Fld, Pill, Vazio, msgErro } from "@/components/ui/Gestao";
import { useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { CUSTO_TIPOS, ETAPAS_EVENTO, RANK_STATUS, rotulo } from "@/lib/models/dominio";
import type { EventoDetalheResponseDTO, EventoResponseDTO, IndicadoresEventoDTO, LogisticaEventoDTO, PessoaEquipeDTO, ProjetoEventoDTO } from "@/lib/models/evento";
import {
  ajustarEtapa,
  fecharEvento,
  lancarEquipe,
  salvarLogistica,
  salvarOrcamento,
} from "@/lib/api/gestao/eventos";
import { brl, dec, fdate, numIn, pct } from "@/lib/utils/numero";
import { round2 } from "@/lib/utils/datas";
import { EquipeChecklistSheet, PessoaForm } from "./EquipeForms";

/**
 * Evento como projeto (protótipo: evProjetoSection, evEquipeSection, evLogisticaSection,
 * orcamentoForm e fecharEventoSheet). Os números vêm calculados do backend em `det.projeto`;
 * enquanto ele não vier, todas as seções aparecem do mesmo jeito, com os campos vazios.
 */

type Mudar = (fn: () => Promise<unknown>, ok: string) => Promise<void>;
const custoLabel = (k: string) => rotulo(CUSTO_TIPOS, k);
const etapaLabel = (k: string) => rotulo(ETAPAS_EVENTO, k);
const realizadoOuMais = (e: EventoResponseDTO) => RANK_STATUS[e.status] >= RANK_STATUS.realizado;

function Cel({ k, v, s, cls }: { k: string; v: React.ReactNode; s?: React.ReactNode; cls?: string }) {
  return (
    <div>
      <div className="k">{k}</div>
      <div className={`v ${cls ?? ""}`}>{v}</div>
      {s ? <div className="note">{s}</div> : null}
    </div>
  );
}

/** Valor em reais ou "—" quando o servidor ainda não mandou o número. */
const brlN = (n: number | null | undefined) => (n == null ? "—" : brl(n));

/**
 * Indicadores, previsto × realizado e etapas: o topo da ficha do evento. Indicadores e desvios vêm
 * calculados no detalhe do evento (det.indicadores / det.desvios); sem eles, os campos ficam em "—".
 */
export function ProjetoSecoes({ det, mudar }: { det: EventoDetalheResponseDTO; mudar: Mudar }) {
  const { abrir } = useSheet();
  const P = det.projeto ?? null;
  const e = det.evento;
  const I = det.indicadores ?? null;
  const desvios = det.desvios ?? [];
  const realizado = realizadoOuMais(e);
  const temReal = !!I && I.totalRealizado > 0;
  const resultado = I ? (temReal ? I.resultadoRealizado : I.resultadoPrevisto) : null;
  const base = I ? I.contratado || I.receitaPrevista : 0;
  const etapas = det.etapas;
  const feitas = etapas.filter((x) => x.feito || x.dispensada).length;
  const temEscala = det.equipe.length > 0;

  return (
    <>
      <div className="sec">
        <div className="sec-h">
          <h3>Indicadores do evento</h3>
          <span className="note">{realizado ? "realizado" : "previsto até o evento"}</span>
        </div>
        <div className="result evind">
          <Cel k="Valor contratado" v={I ? brl(base) : "—"} s={I?.comExtras ? "com extras" : ""} />
          <Cel k="Recebido" v={brlN(I?.recebido)} s={I?.contratado ? pct(I.recebido / I.contratado) : ""} />
          <Cel k="Saldo a receber" v={brlN(I?.aReceber)} cls={I && I.aReceber > 0 && realizado ? "neg" : ""} />
          <Cel k="A&B previsto" v={brlN(I?.previsto.ab)} s={I && I.convidados && I.previsto.ab ? `${brl(I.previsto.ab / I.convidados)}/conv.` : "fichas técnicas"} />
          <Cel k="A&B realizado" v={brlN(I?.realizado.ab)} s={I?.estoqueGeral ? `inclui ${brl(I.estoqueGeral)} do estoque` : ""} />
          <Cel k="% CMV" v={I ? pct(I.realizado.ab ? I.cmvRealizado : I.cmvPrevisto) : "—"} s={I ? (I.realizado.ab ? "realizado" : "previsto") : ""} />
          <Cel k="Custo de equipe" v={I ? brl(I.realizado.equipe || I.previsto.equipe) : "—"} s={I ? (I.realizado.equipe ? "lançado" : "previsto") : ""} />
          <Cel k="Transporte" v={I ? brl(I.realizado.transporte || I.previsto.transporte) : "—"} s={I ? (I.realizado.transporte ? "lançado" : "previsto") : ""} />
          <Cel
            k="Custo por convidado"
            v={I && I.convidados ? brl((I.totalRealizado || I.totalPrevisto) / I.convidados) : "—"}
            s={I ? (I.convidados ? `${I.convidados} convidados` : "informe convidados") : ""}
          />
          <Cel k="Custo total" v={I ? brl(I.totalRealizado || I.totalPrevisto) : "—"} s={I ? (temReal ? "realizado" : "previsto") : ""} />
          <Cel
            k="Resultado"
            v={brlN(resultado)}
            s={I ? `${temReal ? "realizado" : "previsto"}${base ? ` · margem ${pct(resultado! / base)}` : ""}` : ""}
            cls={resultado == null ? "" : resultado < 0 ? "neg" : "pos"}
          />
          <Cel k="Contas a pagar" v={brlN(I?.aPagar)} s={I ? (I.contasAPagar ? `${I.contasAPagar} em aberto` : "nenhuma") : ""} />
        </div>
      </div>

      <div className="sec">
        <div className="sec-h">
          <h3>Previsto × realizado</h3>
          <button type="button" className="btn sm" onClick={() => abrir("Orçamento do evento", <OrcamentoForm evento={e} projeto={P} indicadores={I} escala={det.equipe} />)}>
            Orçamento
          </button>
        </div>
        <div className="box">
          <div className="tablewrap">
            <table className="pvr">
              <thead>
                <tr>
                  <th>Custo</th>
                  <th className="r">Previsto</th>
                  <th className="r">Realizado</th>
                  <th className="r">Desvio</th>
                </tr>
              </thead>
              <tbody>
                {CUSTO_TIPOS.map(([k, l]) => {
                  const p = I?.previsto[k] ?? 0;
                  const r = I?.realizado[k] ?? 0;
                  const df = round2(r - p);
                  const d = p > 0 ? (r - p) / p : null;
                  return (
                    <tr key={k}>
                      <td>
                        {l}
                        {k === "equipe" && temEscala && <span className="note"> (escala)</span>}
                      </td>
                      <td className="r">{p ? brl(p) : "—"}</td>
                      <td className="r">{r ? brl(r) : "—"}</td>
                      <td className="r">
                        {!r ? (
                          <span className="note">—</span>
                        ) : !p ? (
                          <span className="warn-t">sem previsto</span>
                        ) : (
                          <span className={d! > 0.1 ? "neg" : d! < -0.1 ? "pos" : ""}>
                            {df > 0 ? "+" : ""}
                            {brl(df)}
                            <br />
                            <small>
                              {d! > 0 ? "+" : ""}
                              {pct(d)}
                            </small>
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
                <tr className="sum">
                  <td>Total</td>
                  <td className="r">{brlN(I?.totalPrevisto)}</td>
                  <td className="r">{brlN(I?.totalRealizado)}</td>
                  <td className="r">
                    {I && I.totalRealizado && I.totalPrevisto ? (
                      <span className={(I.totalRealizado - I.totalPrevisto) / I.totalPrevisto > 0.1 ? "neg" : ""}>
                        {I.totalRealizado > I.totalPrevisto ? "+" : ""}
                        {pct((I.totalRealizado - I.totalPrevisto) / I.totalPrevisto)}
                      </span>
                    ) : (
                      "—"
                    )}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        {desvios.length > 0 && (
          <div className="banner">
            <b>Desvio acima de 10%:</b> {desvios.map((x) => `${custoLabel(x.tipo)}${x.desvio == null ? " (sem previsto)" : ` +${pct(x.desvio)}`}`).join(" · ")}. Confira as notas e lançamentos do evento.
          </div>
        )}
        <p className="note">
          Previsto: A&amp;B pelas fichas técnicas do cardápio, equipe pela escala, transporte e outros pelo orçamento. Realizado: custos lançados no evento (notas, equipe, frete) + o que saiu do estoque geral para o evento
          (produção, perdas e sobras pelo custo atual, descontando o que foi comprado por nota para ele) + avarias de louça.
          {I?.avarias ? ` Avarias: ${brl(I.avarias)}.` : ""}
        </p>
      </div>

      <div className="sec">
        <div className="sec-h">
          <h3>Etapas do evento</h3>
          <Pill tom={feitas === etapas.length ? "good" : ""}>
            {feitas} de {etapas.length}
          </Pill>
        </div>
        <div className="box list">
          {etapas.map((x, ix) => {
            const meta = [x.info, x.dispensada ? "dispensada" : null, x.manual && x.em ? `marcada em ${fdate(x.em.slice(0, 10))}` : null].filter(Boolean).join(" · ");
            return (
              <div key={x.etapa} className="row static" style={{ background: x.atrasada ? "var(--warn-soft)" : undefined }}>
                <span className="num note" style={{ width: 22, flex: "none" }}>
                  {ix + 1}
                </span>
                <span className="grow">
                  <span className="t wrap">{etapaLabel(x.etapa)}</span>
                  {(meta || x.atrasada) && (
                    <span className="m wrap">
                      {meta}
                      {x.atrasada && (
                        <span className="warn-t">
                          {meta ? " · " : ""}pendente a {x.diasParaEvento} dia{x.diasParaEvento === 1 ? "" : "s"} do evento
                        </span>
                      )}
                    </span>
                  )}
                </span>
                {x.etapa === "fechamento" && !x.feito ? (
                  <button type="button" className={`btn sm${realizado ? " primary" : ""}`} onClick={() => abrir(`Fechamento · ${e.nome}`, <FechamentoSheet evento={e} projeto={P} indicadores={I} />)}>
                    Fechar
                  </button>
                ) : x.auto ? (
                  <Pill tom="good">feito</Pill>
                ) : (
                  <>
                    <label className="check" style={{ fontSize: 13, fontWeight: 500 }}>
                      <input type="checkbox" checked={x.feito} onChange={(ev) => mudar(() => ajustarEtapa(e.id, { etapa: x.etapa, feito: ev.target.checked }), ev.target.checked ? "Etapa concluída" : "Etapa reaberta")} /> feito
                    </label>
                    {x.etapa === "degustacao" && !x.feito && (
                      <button type="button" className="btn sm ghost" onClick={() => mudar(() => ajustarEtapa(e.id, { etapa: "degustacao", dispensada: !x.dispensada }), x.dispensada ? "Degustação incluída" : "Sem degustação")}>
                        {x.dispensada ? "incluir" : "não terá"}
                      </button>
                    )}
                  </>
                )}
              </div>
            );
          })}
        </div>
        <p className="note">Etapas com selo &quot;feito&quot; são detectadas pelo sistema (contrato, cardápio fechado, fichas completas, escala, assinaturas da cozinha, etapa do evento). As outras você marca.</p>
      </div>
    </>
  );
}

/* --------------------------------- Escala --------------------------------- */

export function EquipeSecao({ det }: { det: EventoDetalheResponseDTO }) {
  const { abrir, recarregar } = useSheet();
  const { showToast } = useToast();
  const [ocupado, setOcupado] = useState(false);
  const e = det.evento;
  const eq = det.equipe;
  const totalEquipe = eq.reduce((s, p) => s + p.valor, 0);
  // Diárias com valor ainda fora do caixa (sem conta a pagar).
  const semLancamento = eq.filter((p) => p.valor > 0 && p.lancamentoId == null).length;
  const porFuncao = Object.entries(eq.reduce<Record<string, number>>((o, p) => ({ ...o, [p.funcao]: (o[p.funcao] ?? 0) + 1 }), {}))
    .map(([f, n]) => `${n} ${f}`)
    .join(" · ");

  async function lancar() {
    setOcupado(true);
    try {
      const r = await lancarEquipe(e.id);
      showToast(`${r.lancados} diária${r.lancados === 1 ? "" : "s"} em contas a pagar · vencimento ${fdate(r.vencimento)}`);
      recarregar();
    } catch (err) {
      showToast(msgErro(err), "error");
    } finally {
      setOcupado(false);
    }
  }

  return (
    <div className="sec">
      <div className="sec-h">
        <h3>Escala de equipe</h3>
        <button type="button" className="btn sm" onClick={() => abrir("Escalar equipe", <EquipeChecklistSheet evento={e} />, { larga: true })}>
          Adicionar
        </button>
      </div>
      <div className="box list">
        {!eq.length ? (
          <Vazio titulo="Ninguém escalado">Em Adicionar, marque as pessoas da equipe e salve de uma vez; depois ajuste função, horários e diária de cada uma. O total vira o custo de equipe previsto e pode ir para contas a pagar.</Vazio>
        ) : (
          <>
            <div className="monthhead">{porFuncao}</div>
            {eq.map((p) => (
              <button key={p.id} type="button" className="row" onClick={() => abrir("Pessoa da escala", <PessoaForm evento={e} pessoa={p} />)}>
                <span className="grow">
                  <span className="t">{p.nome || "(sem nome)"}</span>
                  <span className="m">{[p.funcao, p.chegada || p.saida ? `${p.chegada || "?"}–${p.saida || "?"}` : null, p.contato].filter(Boolean).join(" · ")}</span>
                </span>
                {p.lancamentoId != null && <Pill tom={p.lancamentoStatus === "realizado" ? "good" : ""}>{p.lancamentoStatus === "realizado" ? "pago" : "a pagar"}</Pill>}
                <span className="amt">{p.valor ? brl(p.valor) : "—"}</span>
              </button>
            ))}
          </>
        )}
      </div>
      {eq.length > 0 && (
        <div className="toolbar between">
          <span className="note">
            Total da escala: <b className="num">{brlN(totalEquipe)}</b>
            {semLancamento > 0 && ` · ${semLancamento} ainda fora do caixa`}
          </span>
          {semLancamento > 0 && (
            <button type="button" className="btn sm primary" onClick={lancar} disabled={ocupado}>
              Lançar {semLancamento} em contas a pagar
            </button>
          )}
        </div>
      )}
    </div>
  );
}

/* -------------------------------- Logística -------------------------------- */

const LOGISTICA_VAZIA: LogisticaEventoDTO = { veiculos: "", motorista: "", viagens: "", saida: "", montagem: "", retorno: "", obs: "", atualizadoEm: null };

export function LogisticaSecao({ det }: { det: EventoDetalheResponseDTO }) {
  const { abrir } = useSheet();
  const P = det.projeto ?? null;
  const L = P?.logistica ?? LOGISTICA_VAZIA;
  const planejado = !!(L.saida || L.veiculos || L.montagem || L.obs);
  const ass = [
    ["Carga", P?.assinaturas.carga ?? null],
    ["Recebimento no evento", P?.assinaturas.recebimento ?? null],
    ["Retorno conferido", P?.assinaturas.retorno ?? null],
  ] as const;
  return (
    <div className="sec">
      <div className="sec-h">
        <h3>Logística e transporte</h3>
        <button type="button" className="btn sm" onClick={() => abrir("Logística e transporte", <LogisticaForm evento={det.evento} projeto={P} />)}>
          {L.saida || L.veiculos ? "Editar" : "Planejar"}
        </button>
      </div>
      {planejado ? (
        <>
          <dl className="facts">
            {(
              [
                ["Veículos", L.veiculos],
                ["Motorista", L.motorista],
                ["Saída da fábrica", L.saida],
                ["Montagem no local", L.montagem],
                ["Retorno previsto", L.retorno],
                ["Viagens", L.viagens],
              ] as const
            ).map(([k, val]) => (
              <div key={k}>
                <dt>{k}</dt>
                <dd>{val || "—"}</dd>
              </div>
            ))}
          </dl>
          {L.obs && (
            <p className="note" style={{ whiteSpace: "pre-wrap" }}>
              {L.obs}
            </p>
          )}
        </>
      ) : (
        <div className="box">
          <Vazio titulo="Sem plano de transporte">Defina veículos, motorista, horário de saída e de montagem. O custo previsto do transporte fica no Orçamento.</Vazio>
        </div>
      )}
      {ass.some(([, a]) => a?.por) && (
        <div className="summary">
          <span>
            <b>Assinaturas da cozinha</b>
          </span>
          {ass.map(([k, a]) => (
            <span key={k}>
              {k}:{" "}
              {a?.por ? (
                <>
                  {a.por}
                  {a.hora ? ` · ${a.hora}` : ""}
                </>
              ) : (
                <span className="note">pendente</span>
              )}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

function LogisticaForm({ evento, projeto }: { evento: EventoResponseDTO; projeto: ProjetoEventoDTO | null }) {
  const L = projeto?.logistica ?? LOGISTICA_VAZIA;
  const [v, setV] = useState({ veiculos: L.veiculos, motorista: L.motorista, viagens: L.viagens, saida: L.saida, montagem: L.montagem, retorno: L.retorno, obs: L.obs });
  const set = (k: keyof typeof v) => (x: { target: { value: string } }) => setV({ ...v, [k]: x.target.value });
  return (
    <FormSheet onSalvar={() => salvarLogistica(evento.id, v)} okMsg="Logística salva">
      <Fld label="Veículos" full>
        <input value={v.veiculos} onChange={set("veiculos")} placeholder="Ex.: van refrigerada + Fiorino" autoFocus />
      </Fld>
      <Fld label="Motorista">
        <input value={v.motorista} onChange={set("motorista")} />
      </Fld>
      <Fld label="Viagens">
        <input value={v.viagens} onChange={set("viagens")} placeholder="Ex.: 2 (montagem e serviço)" />
      </Fld>
      <Fld label="Saída da fábrica">
        <input type="time" value={v.saida} onChange={set("saida")} />
      </Fld>
      <Fld label="Início da montagem">
        <input type="time" value={v.montagem} onChange={set("montagem")} />
      </Fld>
      <Fld label="Retorno previsto">
        <input type="time" value={v.retorno} onChange={set("retorno")} />
      </Fld>
      <Fld label="Observações (acesso, doca, elevador, contato do local)" full>
        <textarea value={v.obs} onChange={set("obs")} />
      </Fld>
    </FormSheet>
  );
}

/* -------------------------------- Orçamento -------------------------------- */

function OrcamentoForm({
  evento,
  projeto,
  indicadores,
  escala,
}: {
  evento: EventoResponseDTO;
  projeto: ProjetoEventoDTO | null;
  indicadores: IndicadoresEventoDTO | null;
  escala: PessoaEquipeDTO[];
}) {
  const o = projeto?.orcamento ?? null;
  const nEscala = escala.length;
  const temEscala = nEscala > 0;
  // Com escala, o custo de equipe previsto é o total das diárias (os indicadores trazem o mesmo número).
  const equipeDaEscala = indicadores?.previsto.equipe ?? escala.reduce((s, p) => s + p.valor, 0);
  const [v, setV] = useState({
    equipe: temEscala ? dec(equipeDaEscala) : dec(o?.equipe ?? ""),
    transporte: dec(o?.transporte || ""),
    outros: dec(o?.outros || ""),
  });
  const set = (k: keyof typeof v) => (x: { target: { value: string } }) => setV({ ...v, [k]: x.target.value });
  return (
    <FormSheet
      intro="Custos previstos que não saem das fichas técnicas. Servem de base para o previsto × realizado e para o alerta de desvio."
      onSalvar={() => salvarOrcamento(evento.id, { equipe: temEscala ? null : round2(numIn(v.equipe)), transporte: round2(numIn(v.transporte)), outros: round2(numIn(v.outros)) })}
      okMsg="Orçamento salvo"
    >
      <Fld label="A&B previsto pelas fichas" hint="Calculado pelo cardápio; muda quando o cardápio ou os preços mudam.">
        <input value={brlN(indicadores?.previsto.ab)} disabled />
      </Fld>
      <Fld label="Equipe (R$)" hint={temEscala ? `Vem da escala (${nEscala} pessoas). Edite na escala.` : "Use enquanto a escala não estiver montada."}>
        <input inputMode="decimal" placeholder="0,00" value={v.equipe} onChange={set("equipe")} disabled={temEscala} />
      </Fld>
      <Fld label="Transporte e logística (R$)">
        <input inputMode="decimal" placeholder="0,00" value={v.transporte} onChange={set("transporte")} />
      </Fld>
      <Fld label="Locação e outros (R$)" hint="Mobiliário, louça alugada, gelo, decoração a cargo do buffet.">
        <input inputMode="decimal" placeholder="0,00" value={v.outros} onChange={set("outros")} />
      </Fld>
    </FormSheet>
  );
}

/* ------------------------------- Fechamento -------------------------------- */

function FechamentoSheet({ evento, projeto, indicadores }: { evento: EventoResponseDTO; projeto: ProjetoEventoDTO | null; indicadores: IndicadoresEventoDTO | null }) {
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [ocupado, setOcupado] = useState(false);
  const I = indicadores;
  const pend = projeto?.pendenciasFechamento ?? [];

  async function confirmar() {
    setOcupado(true);
    try {
      await fecharEvento(evento.id);
      showToast("Evento fechado");
      recarregar();
      fechar();
    } catch (err) {
      showToast(msgErro(err), "error");
      setOcupado(false);
    }
  }

  return (
    <>
      <div className="result">
        <Cel k="Receita" v={brlN(I?.contratado)} />
        <Cel k="Custo realizado" v={brlN(I?.totalRealizado)} />
        <Cel k="Resultado" v={brlN(I?.resultadoRealizado)} />
        <Cel k="Margem" v={I ? pct(I.contratado ? I.resultadoRealizado / I.contratado : null) : "—"} />
      </div>
      {!projeto ? null : pend.length ? (
        <div className="banner">
          <b>Antes de fechar, confira:</b>
          <ul style={{ margin: "6px 0 0", paddingLeft: 18 }}>
            {pend.map((t) => (
              <li key={t}>{t}</li>
            ))}
          </ul>
        </div>
      ) : (
        <div className="summary">
          <b>Tudo conferido.</b>
          <span>Recebido, contas pagas e custos dentro do previsto.</span>
        </div>
      )}
      <p className="note">
        Fechar passa o evento para <b>Acertado</b> e guarda um retrato do resultado (receita, custos previsto e realizado, margem) com data e quem fechou. Lançamentos posteriores continuam entrando normalmente.
      </p>
      <div className="actions">
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="button" className="btn primary" onClick={confirmar} disabled={ocupado}>
          {pend.length ? "Fechar mesmo assim" : "Fechar evento"}
        </button>
      </div>
    </>
  );
}
