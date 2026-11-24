"use client";

import { useState } from "react";
import { Carregando, Chips, Datebox, FormSheet, Fld, Kpi, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { DN_OPCOES, ETAPAS_ORDEM, ITENS_LIBERACAO, MOTIVOS_BLOQUEIO, SETORES_PADRAO, dnLabel, rotulo, type MotivoBloqueio } from "@/lib/models/dominio";
import type { AcaoOrdemRequestDTO, CozinhaProjecaoDTO, CozinhaResponseDTO, LiberacaoExpedicaoDTO, OrdemProducaoDTO, SetorProducaoDTO } from "@/lib/models/cozinha";
import {
  agirOrdem,
  autorizarLiberacao,
  desfazerLiberacao,
  gerarMapa,
  liberarMapa,
  planejarOrdem,
  salvarLiberacao,
  salvarMestre,
  salvarSetores,
  telaCozinha,
} from "@/lib/api/gestao/cozinha";
import { dLabel, fdate, fmtN } from "@/lib/utils/numero";
import { diffDays, sum } from "@/lib/utils/datas";
import { hm, minutosEntre, situacaoOrdem } from "@/lib/utils/producao";
import { qU, useCozinha } from "./comum";

/**
 * Produção por processos (protótipo: czOrdens, opSheet, opAgir, czLiberacao).
 * As ordens vêm geradas do backend a partir do cardápio confirmado e das fichas técnicas;
 * aqui a equipe registra a execução e a gestão ajusta o plano.
 */

const NOME_KEY = "anne:nome";
/** Nome de quem está registrando neste aparelho (a cozinha usa acesso compartilhado). */
export function lerNome(): string {
  try {
    return window.localStorage.getItem(NOME_KEY) ?? "";
  } catch {
    return "";
  }
}
function guardarNome(n: string) {
  try {
    window.localStorage.setItem(NOME_KEY, n);
  } catch {
    // preferência do aparelho: sem armazenamento, pede o nome de novo na próxima vez
  }
}

export const etapaOrdemLabel = (k: string) => rotulo(ETAPAS_ORDEM, k);
export function setorNome(setores: SetorProducaoDTO[] | undefined, id: string): string {
  return setores?.find((s) => s.id === id)?.nome ?? SETORES_PADRAO.find((s) => s[0] === id)?.[1] ?? (id || "—");
}
const setoresAtivos = (dados: CozinhaResponseDTO): SetorProducaoDTO[] =>
  (dados.setores?.length ? dados.setores : SETORES_PADRAO.map(([id, nome]) => ({ id, nome, ativo: true }))).filter((s) => s.ativo);
export const ordensDe = (dados: CozinhaResponseDTO, eventoId: number) => (dados.ordens ?? []).filter((o) => o.eventoId === eventoId);
/** Ordens de produção que contam para a liberação (fora finalização e expedição). */
const deProducao = (o: OrdemProducaoDTO) => o.etapa !== "expedicao" && o.etapa !== "montagem";

export function OrdemRow({ o, admin, comEvento }: { o: OrdemProducaoDTO; admin: boolean; comEvento?: boolean }) {
  const { abrir } = useSheet();
  const { dados } = useCozinha();
  const s = situacaoOrdem(o);
  const meta = [comEvento ? `${o.codigo} · ${o.eventoNome}` : null, etapaOrdemLabel(o.etapa), setorNome(dados.setores, o.setor), o.q ? qU(o.q, o.u) : null, o.resp || "sem responsável", o.hora ? `até ${o.hora}` : null]
    .filter(Boolean)
    .join(" · ");
  return (
    <button type="button" className="row" onClick={() => abrir("Ordem de produção", <OrdemSheet eventoId={o.eventoId} k={o.k} admin={admin} />)}>
      <Datebox iso={o.data} />
      <span className="grow">
        <span className="t">{o.nome}</span>
        <span className="m">
          {meta}
          {o.status === "bloqueada" && o.exec.motivo && (
            <>
              {" · "}
              <b className="neg">{o.exec.motivo}</b>
            </>
          )}
        </span>
      </span>
      <Pill tom={s[0]}>{s[1]}</Pill>
    </button>
  );
}

/* ------------------------------- Aba Ordens ------------------------------- */

const VISOES = [
  ["setor", "Por setor"],
  ["mapa", "Mapa por prato"],
  ["crono", "Cronograma"],
] as const;

export function AbaOrdens({ pj }: { pj: CozinhaProjecaoDTO }) {
  const { dados, admin } = useCozinha();
  const { abrir, recarregar } = useSheet();
  const { showToast } = useToast();
  const [visao, setVisao] = useState<(typeof VISOES)[number][0]>("setor");
  const [setor, setSetor] = useState("");
  const pr = pj.producao ?? null;
  const os = ordensDe(dados, pj.eventoId);

  async function mudar(fn: () => Promise<unknown>, ok: string) {
    try {
      await fn();
      showToast(ok);
      recarregar();
    } catch (e) {
      showToast(msgErro(e), "error");
    }
  }

  const mestre = (
    <section className="panel">
      <div className="panel-h">
        <h3>Ficha mestre do evento</h3>
        {admin && pr && (
          <button type="button" className="btn sm" onClick={() => abrir("Ficha mestre · produção", <MestreForm eventoId={pj.eventoId} responsavel={pr.responsavel} horarios={pr.horarios} />)}>
            Editar responsável e horários
          </button>
        )}
      </div>
      <div style={{ padding: "14px 18px" }}>
        <dl className="facts">
          <div>
            <dt>Evento</dt>
            <dd>
              {pj.codigo} · {pj.nome}
            </dd>
          </div>
          <div>
            <dt>Data e local</dt>
            <dd>
              {fdate(pj.data)}
              {pj.hora ? ` às ${pj.hora}` : ""}
              {pj.local ? ` · ${pj.local}` : ""}
            </dd>
          </div>
          <div>
            <dt>Convidados</dt>
            <dd>
              {pj.base || "—"} · produção para {pj.N}
            </dd>
          </div>
          <div>
            <dt>Horários de serviço</dt>
            <dd>{pr?.horarios || "—"}</dd>
          </div>
          <div>
            <dt>Responsável geral</dt>
            <dd>{pr?.responsavel || "—"}</dd>
          </div>
          <div>
            <dt>Cardápio</dt>
            <dd>
              {pj.pratos.filter((x) => x.st === "sim").length} pratos confirmados
              {!pj.fechado && <span className="neg"> · em montagem</span>}
            </dd>
          </div>
        </dl>
        {pj.obs && (
          <p className="note" style={{ marginTop: 12, whiteSpace: "pre-wrap" }}>
            <b>Restrições e observações:</b> {pj.obs}
          </p>
        )}
      </div>
    </section>
  );

  if (!pr)
    return (
      <>
        {mestre}
        <section className="panel">
          <Vazio titulo="Mapa de produção ainda não gerado">
            {admin
              ? "Gere o mapa: o sistema lê o cardápio confirmado e as fichas técnicas e cria as ordens por setor e etapa. A equipe só vê depois que você liberar."
              : "A gestão ainda não gerou as ordens deste evento."}
          </Vazio>
          {admin && (
            <div style={{ padding: "0 18px 18px" }}>
              <button type="button" className="btn primary" onClick={() => mudar(() => gerarMapa(pj.eventoId), "Mapa de produção gerado · só a gestão vê até liberar")}>
                Gerar mapa de produção
              </button>
            </div>
          )}
        </section>
      </>
    );

  // A equipe só vê as ordens depois que a gestão libera o mapa.
  if (!admin && !pr.liberado) return mestre;

  const liberacao = dados.liberacoes?.find((l) => l.eventoId === pj.eventoId) ?? null;

  return (
    <>
      {mestre}
      {admin && (
        <div className="toolbar">
          {pr.liberado ? (
            <>
              <Pill tom="good">Liberado para a equipe em {fdate(pr.liberado.slice(0, 10))}</Pill>
              <button type="button" className="btn sm" onClick={() => mudar(() => liberarMapa({ eventoId: pj.eventoId, liberar: false }), "Ordens recolhidas")}>
                Recolher da equipe
              </button>
            </>
          ) : (
            <>
              <Pill tom="warn">Só a gestão vê · em teste</Pill>
              <button type="button" className="btn sm primary" onClick={() => mudar(() => liberarMapa({ eventoId: pj.eventoId, liberar: true }), "Ordens liberadas para a equipe")}>
                Liberar para a equipe
              </button>
            </>
          )}
          <button type="button" className="btn sm" onClick={() => abrir("Setores da cozinha", <SetoresForm setores={dados.setores ?? []} />)}>
            Setores…
          </button>
          <span className="note">O mapa acompanha o cardápio e as fichas automaticamente.</span>
        </div>
      )}
      {!os.length ? (
        <section className="panel">
          <Vazio titulo="Nenhuma ordem">Confirme pratos no cardápio.</Vazio>
        </section>
      ) : (
        <>
          <div className="kpis">
            <Kpi k="Ordens" v={String(os.length)} h={`${os.filter((o) => o.status === "concluida").length} concluídas`} />
            <Kpi k="Atrasadas" v={String(os.filter((o) => o.atrasada).length)} negativo={os.some((o) => o.atrasada)} h="prazo vencido" />
            <Kpi k="Bloqueadas" v={String(os.filter((o) => o.status === "bloqueada").length)} negativo={os.some((o) => o.status === "bloqueada")} h="com motivo" />
            <Kpi k="Em conferência" v={String(os.filter((o) => o.status === "em_conferencia").length)} h="aguardando o líder" />
          </div>
          <Chips opcoes={VISOES} valor={visao} onChange={setVisao} rotulo="Visão das ordens" />
          {visao === "mapa" && <MapaPorPrato os={os} admin={admin} />}
          {visao === "crono" && <CronogramaOrdens os={os} pj={pj} admin={admin} />}
          {visao === "setor" && <PorSetor os={os} admin={admin} setor={setor} setSetor={setSetor} />}
        </>
      )}
      {liberacao && <LiberacaoPainel pj={pj} l={liberacao} />}
    </>
  );
}

function PorSetor({ os, admin, setor, setSetor }: { os: OrdemProducaoDTO[]; admin: boolean; setor: string; setSetor: (s: string) => void }) {
  const { dados } = useCozinha();
  const ss = [...new Set([...setoresAtivos(dados).map((s) => s.id), ...os.map((o) => o.setor)])].filter((s) => os.some((o) => o.setor === s));
  return (
    <>
      <div className="toolbar" role="group" aria-label="Setor">
        <button type="button" className="chip" aria-pressed={!setor} onClick={() => setSetor("")}>
          Todos
        </button>
        {ss.map((s) => (
          <button key={s} type="button" className="chip" aria-pressed={setor === s} onClick={() => setSetor(s)}>
            {setorNome(dados.setores, s)} · {os.filter((o) => o.setor === s).length}
          </button>
        ))}
      </div>
      {ss
        .filter((s) => !setor || setor === s)
        .map((s) => {
          const l = os.filter((o) => o.setor === s).sort((a, b) => String(a.data).localeCompare(String(b.data)) || a.hora.localeCompare(b.hora));
          return (
            <section key={s} className="panel">
              <div className="panel-h">
                <h3>{setorNome(dados.setores, s)}</h3>
                <span className="note">
                  {l.filter((o) => o.status === "concluida").length}/{l.length} concluídas
                </span>
              </div>
              <div className="panel-b">
                {l.map((o) => (
                  <OrdemRow key={o.k} o={o} admin={admin} />
                ))}
              </div>
            </section>
          );
        })}
    </>
  );
}

function MapaPorPrato({ os, admin }: { os: OrdemProducaoDTO[]; admin: boolean }) {
  const principais = os.filter((o) => o.etapa === "principal");
  const exp = os.find((o) => o.etapa === "expedicao");
  return (
    <>
      {principais.map((m) => {
        const rel = os.filter((o) => o !== m && (o.k === `f_${m.cid}` || ((o.etapa === "pre" || o.etapa === "molho") && o.origem.includes(m.nome))));
        const ord = [m, ...rel.filter((o) => o.etapa === "pre"), ...rel.filter((o) => o.etapa === "molho"), ...rel.filter((o) => o.etapa === "montagem")];
        return (
          <section key={m.k} className="panel">
            <div className="panel-h">
              <h3>{m.nome}</h3>
              <span className="note">{qU(m.q, m.u)}</span>
            </div>
            <div className="panel-b">
              {ord.map((o) => (
                <OrdemRow key={o.k} o={o} admin={admin} />
              ))}
              {m.comp.length > 0 && (
                <div className="row static">
                  <span className="grow">
                    <span className="t">Componentes que viajam separados</span>
                    <span className="m">{m.comp.map((k) => `${k.n} ${qU(k.q, k.u)}`).join(" · ")}</span>
                  </span>
                </div>
              )}
            </div>
          </section>
        );
      })}
      {exp && (
        <section className="panel">
          <div className="panel-h">
            <h3>Acondicionamento e expedição</h3>
          </div>
          <div className="panel-b">
            <OrdemRow o={exp} admin={admin} />
          </div>
        </section>
      )}
    </>
  );
}

function CronogramaOrdens({ os, pj, admin }: { os: OrdemProducaoDTO[]; pj: CozinhaProjecaoDTO; admin: boolean }) {
  const dias = new Map<string, OrdemProducaoDTO[]>();
  for (const o of os) dias.set(o.data ?? "sem", [...(dias.get(o.data ?? "sem") ?? []), o]);
  const carga = (o: OrdemProducaoDTO) => o.tempoMin || 30;
  const tot = sum(os, carga);
  return (
    <>
      {[...dias.keys()].sort().map((d) => {
        const l = dias.get(d)!;
        const pc = tot ? sum(l, carga) / tot : 0;
        const concentrado = pc > 0.45 && l.length > 2;
        return (
          <section key={d} className="panel">
            <div className="panel-h">
              <h3>{d === "sem" ? "Sem data" : `${dLabel(d)} · ${dnLabel(diffDays(pj.data, d))}`}</h3>
              <Pill tom={concentrado ? "warn" : ""}>
                {l.length} ordens · {Math.round(pc * 100)}% da carga{concentrado ? " · concentrado" : ""}
              </Pill>
            </div>
            <div className="panel-b">
              {l.map((o) => (
                <OrdemRow key={o.k} o={o} admin={admin} />
              ))}
            </div>
          </section>
        );
      })}
      <p className="note">Carga = tempo estimado das fichas (30 min quando a ficha não informa). Dia com mais de 45% da carga aparece como concentrado: antecipe pré-preparos e molhos.</p>
    </>
  );
}

/* ------------------------------ Folha da ordem ------------------------------ */

/** Detalhe da ordem, sempre lido de novo do BFF (a folha fica fora do contexto da tela). */
export function OrdemSheet({ eventoId, k, admin }: { eventoId: number; k: string; admin: boolean }) {
  const { dados, erro } = useDados(telaCozinha);
  const { abrir } = useSheet();
  if (!dados) return <Carregando erro={erro} />;
  const o = (dados.ordens ?? []).find((x) => x.eventoId === eventoId && x.k === k);
  if (!o) return <Vazio titulo="Ordem não encontrada">O cardápio ou o mapa mudou. Feche e abra de novo.</Vazio>;
  const x = o.exec;
  const s = situacaoOrdem(o);
  const dif = x.qtd != null ? x.qtd - o.q : null;
  const tReal = minutosEntre(x.iniciadoEm, x.concluidoEm);
  const ab = (acao: AcaoOrdemRequestDTO["acao"]) => abrir(`${tituloAcao[acao]} · ${o.nome}`, <AcaoOrdemForm o={o} acao={acao} />);
  return (
    <>
      <div className="sec">
        <div className="toolbar between">
          <Pill>
            {etapaOrdemLabel(o.etapa)} · {setorNome(dados.setores, o.setor)}
          </Pill>
          <Pill tom={s[0]}>{s[1]}</Pill>
        </div>
        <h2>{o.nome}</h2>
        <p className="note">
          {o.codigo} · {o.eventoNome} · {fdate(o.eventoData)}
          {o.origem.length > 0 && o.etapa !== "principal" ? ` · usado em: ${o.origem.join(", ")}` : ""}
        </p>
      </div>
      <dl className="facts">
        {o.q > 0 && (
          <div>
            <dt>Quantidade necessária</dt>
            <dd className="num">{qU(o.q, o.u)}</dd>
          </div>
        )}
        {o.receitas > 0 && (
          <div>
            <dt>Receitas</dt>
            <dd className="num">
              {fmtN(o.receitas)} × rende {fmtN(o.rend)} {o.rendU}
            </dd>
          </div>
        )}
        <div>
          <dt>Prazo</dt>
          <dd>
            {dnLabel(o.dn)}
            {o.data ? ` · ${dLabel(o.data)}` : ""}
            {o.hora ? ` até ${o.hora}` : ""}
            {o.dnPadrao && <span className="note"> (padrão)</span>}
          </dd>
        </div>
        <div>
          <dt>Responsável</dt>
          <dd>{o.resp || "—"}</dd>
        </div>
        {o.tempoMin > 0 && (
          <div>
            <dt>Tempo estimado</dt>
            <dd>{o.tempoMin} min</dd>
          </div>
        )}
        {o.equip && (
          <div>
            <dt>Equipamentos</dt>
            <dd>{o.equip}</dd>
          </div>
        )}
        <div>
          <dt>Armazenamento</dt>
          <dd>{o.armaz || "—"}</dd>
        </div>
        {(o.etapa === "principal" || o.etapa === "pre" || o.etapa === "molho") && (
          <div>
            <dt>Validade depois de pronto</dt>
            <dd>
              {o.valDias} dia{o.valDias === 1 ? "" : "s"}
            </dd>
          </div>
        )}
      </dl>
      <div className="toolbar">
        {o.status === "pendente" && (
          <>
            <button type="button" className="btn sm primary" onClick={() => ab("iniciar")}>
              Iniciar
            </button>
            <button type="button" className="btn sm" onClick={() => ab("bloquear")}>
              Bloquear
            </button>
          </>
        )}
        {o.status === "em_producao" && (
          <>
            <button type="button" className="btn sm primary" onClick={() => ab("concluir")}>
              Concluir execução
            </button>
            <button type="button" className="btn sm" onClick={() => ab("bloquear")}>
              Bloquear
            </button>
          </>
        )}
        {o.status === "em_conferencia" && (
          <>
            <button type="button" className="btn sm primary" onClick={() => ab("conferir")}>
              Conferir e aprovar
            </button>
            <AcaoDireta o={o} acao="reabrir" rotulo="Devolver para produção" />
          </>
        )}
        {o.status === "bloqueada" && <AcaoDireta o={o} acao="desbloquear" rotulo="Desbloquear" primaria />}
        {o.status === "concluida" && admin && <AcaoDireta o={o} acao="reabrir" rotulo="Reabrir" />}
        {admin ? (
          <button type="button" className="btn sm ghost" onClick={() => abrir(`Plano · ${o.nome}`, <PlanoOrdemForm o={o} setores={setoresAtivos(dados)} />)}>
            Prazo, setor e responsável
          </button>
        ) : (
          <button type="button" className="btn sm ghost" onClick={() => ab("responsavel")}>
            Responsável
          </button>
        )}
      </div>
      {o.ins.length > 0 && (
        <div className="sec">
          <h3>Ingredientes e componentes</h3>
          <div className="box tablewrap">
            <table>
              <tbody>
                {o.ins.map((i, ix) => (
                  <tr key={ix}>
                    <td className="wrapc">
                      {i.sub ? (
                        <>
                          ↳ <b>{i.n}</b> <span className="note">(sub-receita, ordem própria)</span>
                        </>
                      ) : (
                        i.n
                      )}
                    </td>
                    <td className="r">
                      <b>{qU(i.q, i.u)}</b>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
      {o.comp.length > 0 && (
        <div className="sec">
          <h3>Viajam separados</h3>
          <p className="note">{o.comp.map((c) => `${c.n} ${qU(c.q, c.u)}`).join(" · ")}</p>
        </div>
      )}
      {o.passos.length > 0 ? (
        <div className="sec">
          <h3>Como executar</h3>
          <ol style={{ margin: 0, paddingLeft: 20, lineHeight: 1.55 }}>
            {o.passos.map((p, ix) => (
              <li key={ix}>{p}</li>
            ))}
          </ol>
        </div>
      ) : (
        o.etapa !== "expedicao" && <p className="note">A ficha técnica não tem etapas de preparo. Cadastre em Fichas para aparecerem aqui.</p>
      )}
      {(x.iniciadoEm || x.concluidoEm || o.status === "bloqueada") && (
        <div className="sec">
          <h3>Execução</h3>
          <dl className="facts">
            <div>
              <dt>Iniciou</dt>
              <dd>
                {x.iniciadoPor || "—"} · {hm(x.iniciadoEm)}
              </dd>
            </div>
            <div>
              <dt>Concluiu</dt>
              <dd>
                {x.concluidoPor || "—"} · {hm(x.concluidoEm)}
              </dd>
            </div>
            {x.qtd != null && (
              <div>
                <dt>Produzido</dt>
                <dd className="num">
                  {qU(x.qtd, o.u)}
                  {dif != null && o.q > 0 && (
                    <span className={dif < 0 ? "neg" : ""}>
                      {" "}
                      · {dif > 0 ? "+" : ""}
                      {fmtN(dif)} do previsto
                    </span>
                  )}
                </dd>
              </div>
            )}
            {x.perda > 0 && (
              <div>
                <dt>Perda</dt>
                <dd className="neg">{qU(x.perda, o.u)}</dd>
              </div>
            )}
            {tReal != null && (
              <div>
                <dt>Tempo real</dt>
                <dd>
                  {tReal} min{o.tempoMin ? ` (estimado ${o.tempoMin})` : ""}
                </dd>
              </div>
            )}
            {x.conferidoPor && (
              <div>
                <dt>Conferiu</dt>
                <dd>
                  {x.conferidoPor} · {hm(x.conferidoEm)}
                </dd>
              </div>
            )}
            {o.status === "bloqueada" && (
              <div style={{ gridColumn: "1/-1" }}>
                <dt>Bloqueio</dt>
                <dd className="neg">
                  {x.motivo}
                  {x.motivoTxt ? ` · ${x.motivoTxt}` : ""}
                </dd>
              </div>
            )}
          </dl>
          {x.obs && (
            <p className="note" style={{ whiteSpace: "pre-wrap" }}>
              {x.obs}
            </p>
          )}
        </div>
      )}
      {o.etiqueta && o.status === "concluida" && (
        <div className="sec">
          <h3>Identificação e rastreabilidade</h3>
          <div className="box" style={{ padding: "12px 14px", whiteSpace: "pre-wrap", fontSize: 14.5 }}>
            {o.etiqueta}
          </div>
        </div>
      )}
      {x.hist.length > 0 && (
        <details className="ckreg">
          <summary>Histórico ({x.hist.length})</summary>
          <ol>
            {[...x.hist].reverse().map((r, ix) => (
              <li key={ix}>
                {hm(r.em)} · {r.quem || "—"} · {r.acao}
              </li>
            ))}
          </ol>
        </details>
      )}
    </>
  );
}

const tituloAcao: Record<AcaoOrdemRequestDTO["acao"], string> = {
  iniciar: "Iniciar",
  concluir: "Concluir",
  conferir: "Conferir",
  bloquear: "Bloquear",
  desbloquear: "Desbloquear",
  reabrir: "Reabrir",
  responsavel: "Responsável",
};

/** Ações sem formulário (desbloquear, reabrir): gravam direto. */
function AcaoDireta({ o, acao, rotulo: r, primaria }: { o: OrdemProducaoDTO; acao: "desbloquear" | "reabrir"; rotulo: string; primaria?: boolean }) {
  const { recarregar } = useSheet();
  const { showToast } = useToast();
  const [ocupado, setOcupado] = useState(false);
  return (
    <button
      type="button"
      className={`btn sm${primaria ? " primary" : ""}`}
      disabled={ocupado}
      onClick={async () => {
        setOcupado(true);
        try {
          await agirOrdem({ acao, eventoId: o.eventoId, k: o.k, quem: lerNome() });
          showToast(acao === "desbloquear" ? "Ordem desbloqueada" : "Ordem reaberta");
          recarregar();
        } catch (e) {
          showToast(msgErro(e), "error");
        } finally {
          setOcupado(false);
        }
      }}
    >
      {r}
    </button>
  );
}

function AcaoOrdemForm({ o, acao }: { o: OrdemProducaoDTO; acao: "iniciar" | "concluir" | "conferir" | "bloquear" | "responsavel" | "desbloquear" | "reabrir" }) {
  const x = o.exec;
  const [v, setV] = useState({
    quem: lerNome(),
    resp: o.resp || lerNome(),
    qtd: String(x.qtd ?? o.q).replace(".", ","),
    perda: x.perda ? String(x.perda).replace(".", ",") : "",
    obs: x.obs,
    motivo: "insumo" as MotivoBloqueio,
    texto: "",
  });
  const set = (k: keyof typeof v) => (e: { target: { value: string } }) => setV({ ...v, [k]: e.target.value });
  const num = (s: string) => Number(s.replace(/\./g, "").replace(",", ".")) || 0;
  const dto = (): AcaoOrdemRequestDTO => {
    const base = { eventoId: o.eventoId, k: o.k, quem: v.quem.trim() };
    if (acao === "iniciar") return { ...base, acao, resp: v.resp.trim() || v.quem.trim() };
    if (acao === "concluir") return { ...base, acao, qtd: o.q ? num(v.qtd) : null, perda: o.q ? num(v.perda) : 0, obs: v.obs };
    if (acao === "bloquear") return { ...base, acao, motivo: v.motivo, texto: v.texto.trim() };
    if (acao === "responsavel") return { ...base, acao, resp: v.resp.trim() };
    return { ...base, acao };
  };
  const intro =
    acao === "concluir" && o.q ? (
      <>
        Previsto: <b>{qU(o.q, o.u)}</b>.
      </>
    ) : acao === "conferir" && x.qtd != null ? (
      <>
        Produzido <b>{qU(x.qtd, o.u)}</b> por {x.concluidoPor || "—"}. Ao aprovar, o lote é gerado e{" "}
        {o.etapa === "principal" || o.etapa === "pre" || o.etapa === "molho" ? "os ingredientes saem do estoque." : "a ordem é encerrada."}
      </>
    ) : undefined;
  return (
    <FormSheet
      intro={intro}
      validar={() =>
        !v.quem.trim()
          ? "Informe seu nome."
          : acao === "concluir" && o.q && !(num(v.qtd) > 0)
            ? "Informe a quantidade produzida."
            : acao === "bloquear" && !v.texto.trim()
              ? "Descreva o impedimento."
              : acao === "responsavel" && !v.resp.trim()
                ? "Informe o responsável."
                : null
      }
      onSalvar={async () => {
        guardarNome(v.quem.trim());
        return agirOrdem(dto());
      }}
      okMsg={{ iniciar: "Produção iniciada", concluir: "Enviado para conferência", conferir: "Ordem concluída", bloquear: "Ordem bloqueada", responsavel: "Responsável definido", desbloquear: "Ordem desbloqueada", reabrir: "Ordem reaberta" }[acao]}
      submit={tituloAcao[acao]}
    >
      <Fld label="Seu nome" obrig>
        <input value={v.quem} onChange={set("quem")} placeholder="Quem está registrando" autoFocus />
      </Fld>
      {(acao === "iniciar" || acao === "responsavel") && (
        <Fld label={acao === "iniciar" ? "Responsável pela ordem" : "Responsável"} obrig={acao === "responsavel"}>
          <input value={v.resp} onChange={set("resp")} />
        </Fld>
      )}
      {acao === "concluir" && o.q > 0 && (
        <>
          <Fld label={`Quantidade produzida (${o.u})`} obrig>
            <input inputMode="decimal" value={v.qtd} onChange={set("qtd")} />
          </Fld>
          <Fld label={`Perda ou descarte (${o.u})`}>
            <input inputMode="decimal" value={v.perda} onChange={set("perda")} />
          </Fld>
        </>
      )}
      {acao === "concluir" && (
        <Fld label="Observações e ocorrências" full>
          <textarea value={v.obs} onChange={set("obs")} />
        </Fld>
      )}
      {acao === "bloquear" && (
        <>
          <Fld label="Motivo">
            <Sel value={v.motivo} onChange={(m) => setV({ ...v, motivo: m as MotivoBloqueio })} opcoes={MOTIVOS_BLOQUEIO} />
          </Fld>
          <Fld label="O que está faltando" obrig full>
            <input value={v.texto} onChange={set("texto")} />
          </Fld>
        </>
      )}
    </FormSheet>
  );
}

/* ----------------------------- Gestão do mapa ----------------------------- */

function PlanoOrdemForm({ o, setores }: { o: OrdemProducaoDTO; setores: SetorProducaoDTO[] }) {
  const [v, setV] = useState({ dn: String(o.dn), hora: o.hora, setor: o.setor, resp: o.resp });
  return (
    <FormSheet onSalvar={() => planejarOrdem({ eventoId: o.eventoId, k: o.k, dn: Number(v.dn), hora: v.hora, setor: v.setor, resp: v.resp.trim() })} okMsg="Plano salvo">
      <Fld label="Prazo">
        <Sel value={v.dn} onChange={(x) => setV({ ...v, dn: x })} opcoes={DN_OPCOES.map(([d, l]) => [String(d), l] as const)} />
      </Fld>
      <Fld label="Horário limite">
        <input type="time" value={v.hora} onChange={(e) => setV({ ...v, hora: e.target.value })} />
      </Fld>
      <Fld label="Setor">
        <Sel value={v.setor} onChange={(x) => setV({ ...v, setor: x })} opcoes={setores.map((s) => [s.id, s.nome] as const)} />
      </Fld>
      <Fld label="Responsável">
        <input value={v.resp} onChange={(e) => setV({ ...v, resp: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}

function MestreForm({ eventoId, responsavel, horarios }: { eventoId: number; responsavel: string; horarios: string }) {
  const [v, setV] = useState({ responsavel, horarios });
  return (
    <FormSheet onSalvar={() => salvarMestre({ eventoId, responsavel: v.responsavel.trim(), horarios: v.horarios.trim() })} okMsg="Ficha mestre salva">
      <Fld label="Responsável geral pela produção" full>
        <input value={v.responsavel} onChange={(e) => setV({ ...v, responsavel: e.target.value })} autoFocus />
      </Fld>
      <Fld label="Horários de serviço" full>
        <input value={v.horarios} onChange={(e) => setV({ ...v, horarios: e.target.value })} placeholder="Ex.: recepção 18h, jantar 20h30, doces 22h" />
      </Fld>
    </FormSheet>
  );
}

function SetoresForm({ setores }: { setores: SetorProducaoDTO[] }) {
  const base = setores.length ? setores : SETORES_PADRAO.map(([id, nome]) => ({ id, nome, ativo: true }));
  const [l, setL] = useState(base.map((s) => ({ ...s })));
  const [novo, setNovo] = useState("");
  return (
    <FormSheet
      intro="Renomeie conforme a estrutura real. Para criar um setor novo, escreva o nome na última linha. Setor desativado some das opções, mas as ordens já ligadas a ele continuam. Cada ficha técnica pode indicar o seu setor."
      validar={() => (l.some((s) => !s.nome.trim()) ? "Informe o nome de todos os setores." : null)}
      onSalvar={() => salvarSetores({ setores: [...l.map((s) => ({ id: s.id, nome: s.nome.trim(), ativo: s.ativo })), ...(novo.trim() ? [{ id: "", nome: novo.trim(), ativo: true }] : [])] })}
      okMsg="Setores salvos"
    >
      {l.map((s, ix) => (
        <div key={s.id} className="fld full" style={{ display: "flex", flexDirection: "row", gap: 10, alignItems: "center" }}>
          <input value={s.nome} aria-label={`Nome do setor ${s.id}`} onChange={(e) => setL(l.map((x, i) => (i === ix ? { ...x, nome: e.target.value } : x)))} style={{ flex: 1 }} />
          <label className="check" style={{ fontSize: 13, fontWeight: 500 }}>
            <input type="checkbox" checked={s.ativo} onChange={(e) => setL(l.map((x, i) => (i === ix ? { ...x, ativo: e.target.checked } : x)))} /> ativo
          </label>
        </div>
      ))}
      <Fld label="Novo setor" full>
        <input value={novo} onChange={(e) => setNovo(e.target.value)} />
      </Fld>
    </FormSheet>
  );
}

/* ------------------------- Liberação para expedição ------------------------- */

function LiberacaoPainel({ pj, l }: { pj: CozinhaProjecaoDTO; l: LiberacaoExpedicaoDTO }) {
  const { dados, admin } = useCozinha();
  const { abrir, recarregar } = useSheet();
  const { showToast } = useToast();
  const [hora, setHora] = useState(l.horaSaida);
  const autorizado = !!l.autorizadoEm;
  const os = ordensDe(dados, pj.eventoId).filter(deProducao);

  async function gravar(fn: () => Promise<unknown>, ok?: string) {
    try {
      await fn();
      if (ok) showToast(ok);
      recarregar();
    } catch (e) {
      showToast(msgErro(e), "error");
    }
  }

  return (
    <section className="panel">
      <div className="panel-h">
        <h3>Liberação para expedição</h3>
        <Pill tom={l.situacao[0]}>{l.situacao[1]}</Pill>
      </div>
      <div style={{ padding: "14px 18px", display: "flex", flexDirection: "column", gap: 10 }}>
        <p className="note">
          Ordens de produção concluídas:{" "}
          <b>
            {l.ordensProducao - l.ordensAbertas} de {l.ordensProducao || os.length}
          </b>{" "}
          · Itens do checklist de envio prontos:{" "}
          <b>
            {l.checklistProntos} de {l.checklistTotal}
          </b>
        </p>
        {ITENS_LIBERACAO.map(([k, rot]) => (
          <label key={k} className="check">
            <input type="checkbox" checked={!!l.ck[k]} disabled={autorizado} onChange={(e) => gravar(() => salvarLiberacao({ eventoId: pj.eventoId, ck: { ...l.ck, [k]: e.target.checked } }))} /> {rot}
          </label>
        ))}
        <div className="toolbar">
          <label className="fator">
            <span className="m">Horário de saída</span>
            <input type="time" className="sel-inline" value={hora} disabled={autorizado} onChange={(e) => setHora(e.target.value)} onBlur={() => hora !== l.horaSaida && gravar(() => salvarLiberacao({ eventoId: pj.eventoId, horaSaida: hora }))} />
          </label>
        </div>
        {autorizado ? (
          <>
            <p className="note">
              Autorizado por <b>{l.autorizadoPor}</b> em {hm(l.autorizadoEm)}
              {l.pendencias && (
                <>
                  <br />
                  <b>Pendências justificadas:</b> {l.pendencias}
                </>
              )}
            </p>
            <div>
              <button type="button" className="btn sm" onClick={() => gravar(() => desfazerLiberacao(pj.eventoId), "Liberação desfeita")}>
                Desfazer liberação
              </button>
            </div>
          </>
        ) : (
          <div className="toolbar">
            <button type="button" className="btn sm primary" onClick={() => abrir(l.ok ? "Liberar saída" : "Liberar com pendências", <AutorizarForm eventoId={pj.eventoId} ok={l.ok} />)}>
              {l.ok ? "Liberar saída" : "Liberar com pendências…"}
            </button>
            {!l.ok && l.faltam.length > 0 && <span className="note">Falta: {l.faltam.join(", ")}</span>}
          </div>
        )}
        {!admin && <span className="note">A liberação fica registrada com o nome de quem autorizou.</span>}
      </div>
    </section>
  );
}

function AutorizarForm({ eventoId, ok }: { eventoId: number; ok: boolean }) {
  const [v, setV] = useState({ quem: lerNome(), pendencias: "" });
  return (
    <FormSheet
      intro={ok ? "Tudo conferido." : "A saída fica registrada como liberada com pendências. Justifique cada uma."}
      validar={() => (!v.quem.trim() ? "Informe quem autoriza." : !ok && !v.pendencias.trim() ? "Justifique as pendências." : null)}
      onSalvar={async () => {
        guardarNome(v.quem.trim());
        return autorizarLiberacao({ eventoId, quem: v.quem.trim(), pendencias: ok ? "" : v.pendencias.trim() });
      }}
      okMsg="Saída liberada"
      submit="Liberar"
    >
      <Fld label="Responsável que autoriza" obrig full>
        <input value={v.quem} onChange={(e) => setV({ ...v, quem: e.target.value })} autoFocus />
      </Fld>
      {!ok && (
        <Fld label="Pendências e justificativa" obrig full>
          <textarea value={v.pendencias} onChange={(e) => setV({ ...v, pendencias: e.target.value })} />
        </Fld>
      )}
    </FormSheet>
  );
}

