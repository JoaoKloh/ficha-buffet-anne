"use client";

import { CopiarTexto, Vazio } from "@/components/ui/Gestao";
import type { CozinhaProjecaoDTO, TarefaCozinhaDTO } from "@/lib/models/cozinha";
import { addDays, hoje, segunda } from "@/lib/utils/datas";
import { dLabel, fdate } from "@/lib/utils/numero";
import { PracaChips, ordemPraca, useCozinha, useSalvarExec } from "./comum";

/** Cartão de tarefa: praça, Feito, responsável e ◀ ▶ para mudar o dia. */
export function TarefaCard({ t, mostrarEvento }: { t: TarefaCozinhaDTO; mostrarEvento?: boolean }) {
  const salvar = useSalvarExec();
  const atrasada = !!t.data && t.data < hoje() && !t.feito;
  const chave = `t_${t.eventoId}__${t.k}`;
  const base = { chave, kind: "tarefa" as const, eventoId: t.eventoId, key: t.k };
  return (
    <div className={`task${t.feito ? " done" : ""}${atrasada ? " late" : ""}`}>
      <div className="tr">
        {mostrarEvento && <span className="pchip ev">{t.codigo}</span>}
        <span className="pchip">{t.praca}</span>
        {t.padrao && !t.movida && (
          <span className="pchip warn" title="A ficha não tem etapas: dia sugerido pelo tipo de prato">
            prazo padrão
          </span>
        )}
        <label className="check" style={{ marginLeft: "auto", fontSize: 13 }}>
          <input type="checkbox" checked={t.feito} onChange={(e) => salvar({ ...base, campos: { feito: e.target.checked } })} /> Feito
        </label>
      </div>
      <span className="tt">{t.desc}</span>
      {t.produtos && <span className="m">{t.produtos}</span>}
      {mostrarEvento && (
        <span className="m">
          {t.eventoNome} · evento {fdate(t.eventoData).slice(0, 5)}
        </span>
      )}
      <div className="tr">
        <input
          type="text"
          key={`${chave}-${t.resp}`}
          defaultValue={t.resp}
          placeholder="Responsável"
          aria-label="Responsável"
          onBlur={(e) => e.target.value.trim() !== t.resp && salvar({ ...base, campos: { resp: e.target.value.trim() } })}
        />
        <button type="button" className="btn sm" aria-label="Um dia antes" title="Um dia antes" onClick={() => salvar({ ...base, campos: { dnSet: true, dn: Math.max(0, (t.dnEfetivo ?? 0) + 1) } })}>
          ◀
        </button>
        <button type="button" className="btn sm" aria-label="Um dia depois" title="Um dia depois" disabled={t.dnEfetivo === 0} onClick={() => salvar({ ...base, campos: { dnSet: true, dn: Math.max(0, (t.dnEfetivo ?? 0) - 1) } })}>
          ▶
        </button>
      </div>
    </div>
  );
}

/** Cronograma do evento em colunas D-n, mais "A posicionar". */
export function AbaCronograma({ pj }: { pj: CozinhaProjecaoDTO }) {
  const { dados, praca } = useCozinha();
  const ts = dados.tarefas.filter((t) => t.eventoId === pj.eventoId);
  const vis = ts.filter((t) => !praca || t.praca === praca);
  const maxDn = Math.max(3, ...ts.map((t) => t.dnEfetivo ?? 0));
  const cols: (number | null)[] = [null];
  for (let n = maxDn; n >= 0; n--) cols.push(n);
  const atrasadas = ts.filter((t) => t.data && t.data < hoje() && !t.feito).length;
  const txt = cols
    .map((dn) => {
      const list = vis.filter((t) => t.dnEfetivo === dn);
      if (!list.length) return "";
      const iso = dn == null ? null : addDays(pj.data, -dn);
      return `\n${(dn === null ? "A posicionar" : dn === 0 ? "Dia do evento" : `D-${dn}`).toUpperCase()}${iso ? ` · ${dLabel(iso)}` : ""}\n` + list.map((t) => `[${t.feito ? "x" : " "}] ${t.praca}: ${t.desc}${t.resp ? ` · ${t.resp}` : ""}\n`).join("");
    })
    .reduce((a, b) => a + b, `CRONOGRAMA · ${pj.codigo} · ${pj.nome} · ${fdate(pj.data)}\n`);
  return (
    <>
      <p className="note">
        Tarefas geradas das etapas das fichas dos pratos confirmados, mais as tarefas fixas. Cada tarefa cai no dia = data do evento − antecedência (D-n). Ficha sem etapas gera &quot;Produzir …&quot; com <b>prazo padrão</b> pelo tipo de prato. Use ◀ ▶ para mudar o dia sem alterar a
        ficha.
      </p>
      <div className="summary">
        <span>
          <b className="num">
            {ts.filter((t) => t.feito).length} de {ts.length}
          </b>{" "}
          tarefas feitas{atrasadas ? <span className="neg"> · {atrasadas} atrasada(s)</span> : null}
        </span>
      </div>
      <PracaChips pracas={ts.map((t) => t.praca)} />
      <div className="kcol">
        {cols.map((dn) => {
          const list = vis.filter((t) => t.dnEfetivo === dn).sort((a, b) => ordemPraca(a.praca) - ordemPraca(b.praca));
          if (dn === null && !list.length) return null;
          const iso = dn == null ? null : addDays(pj.data, -dn);
          return (
            <div key={String(dn)} className="box">
              <div className={`dayhd${iso === hoje() ? " today" : ""}`}>
                <span>
                  <span className="m">{dn === null ? "A posicionar" : dn === 0 ? "Dia do evento" : `D-${dn}`}</span>
                  <br />
                  <b className="num">{iso ? dLabel(iso) : "—"}</b>
                </span>
                <span className="m">
                  {list.filter((t) => t.feito).length}/{list.length}
                </span>
              </div>
              {list.length ? list.map((t) => <TarefaCard key={t.k} t={t} />) : <div className="empty" style={{ padding: "12px 14px" }}>Nenhuma tarefa.</div>}
            </div>
          );
        })}
      </div>
      <CopiarTexto texto={txt} rotulo="Copiar como texto" nota="Para colar no WhatsApp da equipe ou do fornecedor." />
    </>
  );
}

/** Agenda da semana: tarefas de todos os eventos por dia, atrasadas e a posicionar. */
export function AbaAgenda({ semana, setSemana }: { semana: string; setSemana: (s: string) => void }) {
  const { dados, praca, abrirEvento } = useCozinha();
  const t = hoje();
  const ini = semana || segunda(t);
  const dias = Array.from({ length: 7 }, (_, i) => addDays(ini, i));
  const vis = dados.tarefas.filter((x) => !praca || x.praca === praca);
  const atras = vis.filter((x) => x.data && x.data < t && !x.feito).sort((a, b) => a.data!.localeCompare(b.data!));
  const semDia = vis.filter((x) => x.dnEfetivo == null && x.eventoData >= t);
  const salvar = useSalvarExec();
  const txt = dias
    .map((d) => {
      const evs = dados.projecoes.filter((p) => p.data === d);
      const list = vis.filter((x) => x.data === d);
      if (!evs.length && !list.length) return "";
      return (
        `\n${dLabel(d).toUpperCase()}\n` +
        evs.map((p) => `  ★ EVENTO ${p.codigo} ${p.nome} · ${p.N} pessoas\n`).join("") +
        list.map((x) => `  [${x.feito ? "x" : " "}] ${x.codigo} · ${x.praca}: ${x.desc}${x.resp ? ` · ${x.resp}` : ""}\n`).join("")
      );
    })
    .reduce((a, b) => a + b, `AGENDA DA COZINHA · ${dLabel(dias[0]!)} a ${dLabel(dias[6]!)}\n`);
  return (
    <>
      <div className="toolbar">
        <button type="button" className="btn sm" aria-label="Semana anterior" onClick={() => setSemana(addDays(ini, -7))}>
          ◀
        </button>
        <b className="num" style={{ minWidth: 170, textAlign: "center" }}>
          {dLabel(dias[0]!)} – {dLabel(dias[6]!)}
        </b>
        <button type="button" className="btn sm" aria-label="Próxima semana" onClick={() => setSemana(addDays(ini, 7))}>
          ▶
        </button>
        <button type="button" className="btn sm ghost" onClick={() => setSemana(segunda(t))}>
          Esta semana
        </button>
      </div>
      <PracaChips pracas={dados.tarefas.map((x) => x.praca)} />
      {atras.length > 0 && (
        <section className="panel">
          <div className="panel-h">
            <h3 className="neg">Atrasadas</h3>
            <span className="pill bad">{atras.length}</span>
          </div>
          <div className="czlate">
            {atras.map((x) => (
              <div key={`${x.eventoId}-${x.k}`} className="lateitem">
                <span className="pchip ev">{x.codigo}</span>
                <span className="grow">
                  {x.desc} <span className="note">· era {dLabel(x.data!)}</span>
                </span>
                <label className="check" style={{ fontSize: 13 }}>
                  <input type="checkbox" onChange={(e) => salvar({ chave: `t_${x.eventoId}__${x.k}`, kind: "tarefa", eventoId: x.eventoId, key: x.k, campos: { feito: e.target.checked } })} /> Feito
                </label>
              </div>
            ))}
          </div>
        </section>
      )}
      <div className="week">
        {dias.map((d) => {
          const evs = dados.projecoes.filter((p) => p.data === d);
          const list = vis.filter((x) => x.data === d).sort((a, b) => a.eventoData.localeCompare(b.eventoData) || ordemPraca(a.praca) - ordemPraca(b.praca));
          return (
            <div key={d} className={`box wday${d === t ? " today" : ""}${!evs.length && !list.length ? " vazio" : ""}`}>
              <div className={`dayhd${d === t ? " today" : ""}`}>
                <span>
                  <b className="num">{dLabel(d)}</b> {d === t && <span className="pill accent">hoje</span>}
                </span>
                <span className="m">{list.length ? `${list.filter((x) => x.feito).length}/${list.length}` : ""}</span>
              </div>
              {evs.map((p) => (
                <button key={p.eventoId} type="button" className="evday" onClick={() => abrirEvento(p.eventoId, "producao")}>
                  <span className="k">Evento</span>
                  <b>
                    {p.codigo} · {p.nome}
                  </b>
                  <span className="m">
                    {p.N} pessoas{p.local ? ` · ${p.local}` : ""}
                  </span>
                </button>
              ))}
              {list.length ? list.map((x) => <TarefaCard key={`${x.eventoId}-${x.k}`} t={x} mostrarEvento />) : !evs.length && <div className="empty" style={{ padding: "10px 14px" }}>Sem tarefas.</div>}
            </div>
          );
        })}
      </div>
      {semDia.length > 0 && (
        <section className="panel">
          <div className="panel-h">
            <h3>A posicionar</h3>
            <span className="pill warn">{semDia.length}</span>
          </div>
          <div className="panel-b">
            <div className="kcol" style={{ padding: 10 }}>
              {semDia.map((x) => (
                <div key={`${x.eventoId}-${x.k}`} className="box">
                  <TarefaCard t={x} mostrarEvento />
                </div>
              ))}
            </div>
          </div>
        </section>
      )}
      {!dados.projecoes.length && <Vazio>Nenhum evento em produção.</Vazio>}
      <CopiarTexto texto={txt} rotulo="Copiar como texto" nota="Para colar no WhatsApp da equipe ou do fornecedor." />
    </>
  );
}
