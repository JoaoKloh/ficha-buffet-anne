"use client";

import { useEffect, useMemo, useState, useSyncExternalStore } from "react";
import { useSearchParams } from "next/navigation";
import { Header } from "@/components/Header";
import { Carregando, Chips, Vazio } from "@/components/ui/Gestao";
import { useDados } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import type { CozinhaProjecaoDTO, CozinhaResponseDTO } from "@/lib/models/cozinha";
import { telaCozinha } from "@/lib/api/gestao/cozinha";
import { diffDays, hoje } from "@/lib/utils/datas";
import { fdate, fmtN } from "@/lib/utils/numero";
import { AbaChecklist } from "./Checklist";
import { AbaCompras, AbaInsumos, AbaProducao, useFiltroCompra } from "./Listas";
import { AbaAgenda, AbaCronograma } from "./Tarefas";
import { Ctx, exDe, useCozinha } from "./comum";
import { czRowSt, czRows } from "@/lib/utils/checklist";
import { statusProducao } from "@/lib/utils/producao";
import { imprimirCozinha, type AbaCozinha } from "./imprimir";
import { AbaIndicadores, AbaLider } from "./Lider";
import { AbaOcorrencias } from "./Ocorrencias";
import { AbaOrdens } from "./Ordens";

const ABAS = [
  ["lider", "Painel do líder"],
  ["ordens", "Ordens de produção"],
  ["agenda", "Agenda da semana"],
  ["producao", "Lista de produção"],
  ["insumos", "Insumos por prato"],
  ["compras", "Lista de compras"],
  ["cronograma", "Cronograma do evento"],
  ["checklist", "Checklist de envio"],
  ["ocorrencias", "Ocorrências"],
  ["indicadores", "Indicadores"],
] as const satisfies readonly (readonly [AbaCozinha, string])[];
const ehAba = (s: string | null | undefined): s is AbaCozinha => ABAS.some((a) => a[0] === s);

const UI_KEY = "anne:cozinha";

/** O que a equipe vê no modo cozinha: ordens e liberação só de mapas liberados (protótipo: czParaEquipe). */
function paraEquipe(d: CozinhaResponseDTO): CozinhaResponseDTO {
  const liberados = new Set(d.projecoes.filter((p) => p.producao?.liberado).map((p) => p.eventoId));
  return {
    ...d,
    ordens: d.ordens?.filter((o) => liberados.has(o.eventoId)),
    liberacoes: d.liberacoes?.filter((l) => liberados.has(l.eventoId)),
  };
}

/**
 * Última aba e evento escolhidos neste aparelho. O servidor não conhece o localStorage:
 * na hidratação o React usa o retrato do servidor (nada salvo) e só depois o do navegador,
 * então o primeiro render bate com o HTML recebido.
 */
const semAssinatura = () => () => {};
function lerUi(): string | null {
  try {
    return window.localStorage.getItem(UI_KEY);
  } catch {
    return null;
  }
}
function useUiSalva(): { aba?: AbaCozinha; evento?: number } {
  const json = useSyncExternalStore(semAssinatura, lerUi, () => null);
  return useMemo(() => {
    try {
      const s = JSON.parse(json || "{}");
      return { aba: ehAba(s?.aba) ? s.aba : undefined, evento: typeof s?.evento === "number" ? s.evento : undefined };
    } catch {
      return {};
    }
  }, [json]);
}

/**
 * Cozinha: o que a equipe vê de cada evento em produção, sem valores.
 * `?modo=cozinha` abre o modo cozinha (só esta tela, sem menu de gestão);
 * `?evento=ID&aba=...` são os atalhos do Painel e da ficha do evento.
 */
export function CozinhaView() {
  const sp = useSearchParams();
  const aba = sp.get("aba");
  const evento = Number(sp.get("evento")) || null;
  const admin = sp.get("modo") !== "cozinha";
  // Um atalho novo na URL remonta a tela já na aba e no evento pedidos.
  return <Tela key={`${aba}|${evento}`} abaUrl={ehAba(aba) ? aba : null} eventoUrl={evento} admin={admin} />;
}

function Tela({ abaUrl, eventoUrl, admin }: { abaUrl: AbaCozinha | null; eventoUrl: number | null; admin: boolean }) {
  const { showToast } = useToast();
  const { dados: tela, erro } = useDados(telaCozinha);
  // Modo cozinha: a equipe só vê as ordens (e a liberação) dos mapas que a gestão já liberou.
  const dados = useMemo(() => (tela && !admin ? paraEquipe(tela) : tela), [tela, admin]);
  // Aba e evento: o atalho da URL vence a última escolha guardada no aparelho.
  const salva = useUiSalva();
  const [abaSt, setAbaSt] = useState<AbaCozinha | null>(abaUrl);
  const [eventoSt, setEventoSt] = useState<number | null>(eventoUrl);
  const aba = abaSt ?? salva.aba ?? "agenda";
  const evento = eventoSt ?? salva.evento ?? null;
  const [semana, setSemana] = useState("");
  const [praca, setPraca] = useState("");
  const [filtroCompra, setFiltroCompra] = useFiltroCompra();

  // Modo cozinha: some o menu da gestão (a classe é lida por gestao.css).
  useEffect(() => {
    if (admin) return;
    document.body.classList.add("modo-cozinha");
    return () => document.body.classList.remove("modo-cozinha");
  }, [admin]);

  function guardar(a: AbaCozinha, e: number | null) {
    try {
      window.localStorage.setItem(UI_KEY, JSON.stringify({ aba: a, evento: e }));
    } catch {
      // preferência de tela: sem armazenamento, fica só nesta visita
    }
  }
  function setAba(a: AbaCozinha) {
    setAbaSt(a);
    guardar(a, evento);
  }

  const pjs = useMemo(() => dados?.projecoes ?? [], [dados]);
  const pj = pjs.find((p) => p.eventoId === evento) ?? pjs.find((p) => p.data >= hoje()) ?? pjs[pjs.length - 1];

  const ctx = useMemo(
    () =>
      dados && {
        dados,
        admin,
        praca,
        setPraca,
        abrirEvento: (id: number, a?: string) => {
          const destino = ehAba(a) ? a : "producao";
          setEventoSt(id);
          setAbaSt(destino);
          guardar(destino, id);
          window.scrollTo(0, 0);
        },
      },
    [dados, admin, praca]
  );

  function imprimir() {
    if (!dados) return;
    try {
      const como = imprimirCozinha(dados, { aba, pj, praca, semana, filtroCompra });
      if (como === "arquivo") showToast("Arquivo salvo: abra e imprima");
    } catch {
      showToast("Não foi possível gerar o arquivo agora.", "error");
    }
  }

  return (
    <div className="app">
      <Header
        sub={admin ? "O que a cozinha vê · sem valores financeiros" : "Área da cozinha · produção, insumos, agenda e conferência"}
        actions={
          <button type="button" className="btn" onClick={imprimir} disabled={!dados}>
            Imprimir / baixar
          </button>
        }
      />
      <Chips opcoes={ABAS} valor={aba} onChange={setAba} rotulo="Seções da cozinha" />
      {!dados || !ctx ? (
        <section className="panel">
          <Carregando erro={erro} />
        </section>
      ) : aba === "lider" ? (
        // O painel do líder tem estrutura própria e aparece mesmo sem eventos (zerado).
        <Ctx.Provider value={ctx}>
          <AbaLider />
        </Ctx.Provider>
      ) : !pjs.length ? (
        <section className="panel">
          <Vazio titulo="Nenhum evento em produção">
            {admin ? "Eventos contratados, com cardápio fechado ou em produção aparecem aqui." : "Os eventos contratados aparecem aqui assim que a gestão abre o sistema."}
          </Vazio>
        </section>
      ) : (
        <Ctx.Provider value={ctx}>
          {aba === "indicadores" ? (
            <AbaIndicadores />
          ) : aba === "agenda" || !pj ? (
            <AbaAgenda semana={semana} setSemana={setSemana} />
          ) : (
            <>
              <div className="toolbar">
                <label className="fator" htmlFor="czev">
                  <span className="m">Evento</span>
                  <select
                    className="sel-inline"
                    id="czev"
                    value={pj.eventoId}
                    onChange={(e) => {
                      setEventoSt(Number(e.target.value));
                      guardar(aba, Number(e.target.value));
                    }}
                  >
                    {pjs.map((p) => (
                      <option key={p.eventoId} value={p.eventoId}>
                        {p.codigo} · {p.nome} · {fdate(p.data)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <Cabecalho pj={pj} />
              {aba === "producao" && <AbaProducao pj={pj} />}
              {aba === "insumos" && <AbaInsumos pj={pj} />}
              {aba === "compras" && <AbaCompras pj={pj} filtro={filtroCompra} setFiltro={setFiltroCompra} />}
              {aba === "cronograma" && <AbaCronograma pj={pj} />}
              {aba === "checklist" && <AbaChecklist pj={pj} />}
              {aba === "ordens" && <AbaOrdens pj={pj} />}
              {aba === "ocorrencias" && <AbaOcorrencias pj={pj} />}
            </>
          )}
        </Ctx.Provider>
      )}
    </div>
  );
}

/** Faixa do evento: para quantas pessoas produzir, quanto falta e os avisos da gestão. */
function Cabecalho({ pj }: { pj: CozinhaProjecaoDTO }) {
  const d = diffDays(pj.data, hoje());
  const inc = pj.pratos.filter((x) => x.st !== "nao" && x.ficha === "Incompleta");
  return (
    <>
      <div className="prev">
        <div style={{ flex: "1 1 260px", minWidth: 0 }}>
          <span className="k">
            {pj.codigo} · {fdate(pj.data)}
            {pj.hora ? ` · ${pj.hora}` : ""}
          </span>
          <b>{pj.nome}</b>
          <span className="m" style={{ display: "block" }}>
            {[pj.local, pj.cidade].filter(Boolean).join(", ")}
          </span>
        </div>
        <div style={{ textAlign: "right", flex: "none" }}>
          <span className="k">Produção para</span>
          <b className="num">{pj.N}</b> <span className="m">pessoas{pj.base ? ` (${pj.base} + ${fmtN(pj.margem)}%)` : ""}</span>
          <br />
          <span className="m">{d > 0 ? `faltam ${d} dia${d > 1 ? "s" : ""}` : d === 0 ? "é hoje" : `foi há ${-d} dia${-d > 1 ? "s" : ""}`}</span>
        </div>
      </div>
      {(!pj.fechado || inc.length > 0 || pj.obs) && (
        <div className="summary">
          {!pj.fechado && (
            <span className="warn-t">
              <b>Cardápio ainda em montagem</b>: quantidades e pratos podem mudar. Produza só o que estiver como Sim.
            </span>
          )}
          {inc.length > 0 && (
            <span className="warn-t">
              {inc.length} prato{inc.length > 1 ? "s" : ""} sem ficha técnica completa:{" "}
              {inc
                .slice(0, 4)
                .map((x) => x.nome)
                .join(", ")}
              {inc.length > 4 ? "…" : ""}. Os insumos desses pratos não aparecem nas listas.
            </span>
          )}
          {pj.obs && (
            <span style={{ whiteSpace: "pre-wrap" }}>
              <b>Observações e restrições</b>
              <br />
              {pj.obs}
            </span>
          )}
        </div>
      )}
      <IndicadoresEvento pj={pj} />
    </>
  );
}

/** Faixa de indicadores do evento (protótipo: czIndicadores). */
function IndicadoresEvento({ pj }: { pj: CozinhaProjecaoDTO }) {
  const { dados } = useCozinha();
  const t = hoje();
  const sim = pj.pratos.filter((x) => x.st === "sim");
  const stp = sim.map((x) => statusProducao(pj, x, exDe(dados, `i_${x.cid}`))[1]);
  const conc = stp.filter((s) => s === "Concluída").length;
  const atr = stp.filter((s) => s === "Atrasada").length + dados.tarefas.filter((x) => x.eventoId === pj.eventoId && x.data && x.data < t && !x.feito && pj.data >= t).length;
  const rows = czRows(pj).map((r) => [r, exDe(dados, r.id)] as const);
  const st = rows.map(([r, x]) => czRowSt(r, x));
  const pend = st.filter((s) => s[1] === "Pendente" || s[1].startsWith("Faltam assinaturas")).length;
  const falhas = st.filter((s) => s[0] === "bad").length;
  const sobras = rows.filter(([r, x]) => r.prod && Number(x[r.retK]) > 0).length;
  const av = pj.loucas.filter((l) => Number(exDe(dados, `l_${pj.eventoId}__${l.id}`).avaria) > 0).length;
  const oc = (dados.ocorrencias ?? []).filter((o) => o.eventoId === pj.eventoId);
  const abertas = oc.filter((o) => !o.resolvida).length;
  const chip = (k: string, v: React.ReactNode, warn?: boolean) => (
    <span className={`pchip${warn ? " warn" : ""}`} style={{ fontSize: 12.5, padding: "4px 10px" }}>
      {k}: <b>{v}</b>
    </span>
  );
  return (
    <div className="toolbar">
      {chip("Produções concluídas", `${conc}/${sim.length}`)}
      {chip("Atrasadas", atr, atr > 0)}
      {chip("Itens pendentes no checklist", pend)}
      {chip("Falhas de carregamento", falhas, falhas > 0)}
      {chip("Sobras e avarias", sobras + av)}
      {chip("Ocorrências", `${oc.length}${abertas ? ` (${abertas} abertas)` : ""}`, abertas > 0)}
    </div>
  );
}
