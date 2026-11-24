"use client";

import { useState } from "react";
import { Chips, Datebox, Kpi, Pill, Vazio } from "@/components/ui/Gestao";
import type { OrdemProducaoDTO } from "@/lib/models/cozinha";
import { brl, pct, qtdU } from "@/lib/utils/numero";
import { addDays, hoje, sum } from "@/lib/utils/datas";
import { OrdemRow, ordensDe, setorNome } from "./Ordens";
import { useCozinha } from "./comum";

/**
 * Painel do líder e indicadores da produção (protótipo: czLider, czIndProd).
 * Filtros e somas simples sobre as ordens que o backend já devolveu com prazo e atraso calculados.
 */

const FILTROS = [
  ["hoje", "Hoje"],
  ["atrasadas", "Atrasadas"],
  ["bloqueadas", "Bloqueadas"],
  ["conferencia", "Aguardando conferência"],
  ["pendentes", "Pendentes"],
  ["concluidas", "Concluídas"],
] as const;
type Filtro = (typeof FILTROS)[number][0];

export function AbaLider() {
  const { dados, admin, abrirEvento } = useCozinha();
  const [f, setF] = useState<Filtro>("hoje");
  const os = dados.ordens ?? [];
  const t = hoje();
  const comOrdens = dados.projecoes.filter((p) => os.some((o) => o.eventoId === p.eventoId));
  // Sem ordens a estrutura do painel continua visível (KPIs zerados, filtros e quadros vazios),
  // como na tela Produção; o aviso de como gerar as ordens vai dentro do quadro das ordens.
  const grupos: Record<Filtro, OrdemProducaoDTO[]> = {
    hoje: os.filter((o) => o.data === t),
    atrasadas: os.filter((o) => o.atrasada),
    bloqueadas: os.filter((o) => o.status === "bloqueada"),
    conferencia: os.filter((o) => o.status === "em_conferencia"),
    pendentes: os.filter((o) => o.status === "pendente"),
    concluidas: os.filter((o) => o.status === "concluida"),
  };
  const abertas = os.filter((o) => o.status !== "concluida");
  const carga = new Map<string, { n: number; min: number }>();
  for (const o of abertas) {
    const c = carga.get(o.setor) ?? { n: 0, min: 0 };
    carga.set(o.setor, { n: c.n + 1, min: c.min + (o.tempoMin || 30) });
  }
  const mx = Math.max(1, ...[...carga.values()].map((c) => c.min));
  const prox = comOrdens.filter((p) => p.data >= t && p.data <= addDays(t, 3));
  return (
    <>
      <div className="kpis">
        <Kpi k="Eventos programados" v={String(comOrdens.length)} h={`${abertas.length} ordens abertas`} />
        <Kpi k="Produções de hoje" v={String(grupos.hoje.length)} h={`${grupos.hoje.filter((o) => o.status === "concluida").length} concluídas`} />
        <Kpi k="Atrasadas" v={String(grupos.atrasadas.length)} negativo={grupos.atrasadas.length > 0} h="prazo vencido" />
        <Kpi k="Bloqueadas" v={String(grupos.bloqueadas.length)} negativo={grupos.bloqueadas.length > 0} h={`${grupos.conferencia.length} aguardando conferência`} />
      </div>
      <Chips opcoes={FILTROS.map(([k, l]) => [k, `${l} · ${grupos[k].length}`] as const)} valor={f} onChange={setF} rotulo="Filtro das ordens" />
      <section className="panel">
        <div className="panel-b">
          {grupos[f].length ? (
            [...grupos[f]].sort((a, b) => String(a.data).localeCompare(String(b.data))).map((o) => <OrdemRow key={`${o.eventoId}|${o.k}`} o={o} admin={admin} comEvento />)
          ) : !os.length ? (
            <Vazio titulo="Nenhum evento com ordens de produção">{admin ? "Abra Ordens de produção num evento e gere o mapa." : "As ordens aparecem quando a gestão libera."}</Vazio>
          ) : (
            <Vazio>Nada aqui.</Vazio>
          )}
        </div>
      </section>
      <div className="grid2">
        <section className="panel">
          <div className="panel-h">
            <h3>Carga por setor</h3>
            <span className="note">ordens abertas</span>
          </div>
          <div style={{ padding: "12px 18px", display: "flex", flexDirection: "column", gap: 10 }}>
            {carga.size ? (
              [...carga.entries()]
                .sort((a, b) => b[1].min - a[1].min)
                .map(([s, c]) => (
                  <div key={s}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 14 }}>
                      <span>{setorNome(dados.setores, s)}</span>
                      <b>
                        {c.n} ordens · {Math.round((c.min / 60) * 10) / 10} h
                      </b>
                    </div>
                    <div className="barra">
                      <div style={{ width: `${Math.round((c.min / mx) * 100)}%` }} />
                    </div>
                  </div>
                ))
            ) : (
              <span className="note">Sem ordens abertas.</span>
            )}
          </div>
        </section>
        <section className="panel">
          <div className="panel-h">
            <h3>Próximos da expedição</h3>
            <span className="note">até 3 dias</span>
          </div>
          <div className="panel-b">
            {prox.length ? (
              prox.map((p) => {
                const l = ordensDe(dados, p.eventoId);
                const lib = dados.liberacoes?.find((x) => x.eventoId === p.eventoId);
                return (
                  <button key={p.eventoId} type="button" className="row" onClick={() => abrirEvento(p.eventoId, "ordens")}>
                    <Datebox iso={p.data} />
                    <span className="grow">
                      <span className="t">
                        {p.codigo} · {p.nome}
                      </span>
                      <span className="m">
                        {l.filter((o) => o.status === "concluida").length}/{l.length} ordens concluídas
                      </span>
                    </span>
                    {lib && <Pill tom={lib.situacao[0]}>{lib.situacao[1]}</Pill>}
                  </button>
                );
              })
            ) : (
              <Vazio>Nenhum evento nos próximos 3 dias.</Vazio>
            )}
          </div>
        </section>
      </div>
    </>
  );
}

export function AbaIndicadores() {
  const { dados, admin } = useCozinha();
  const ind = dados.indicadoresProducao;
  if (!ind || !ind.eventos.length)
    return (
      <section className="panel">
        <Vazio titulo="Sem dados ainda">Os indicadores aparecem quando houver ordens de produção.</Vazio>
      </section>
    );
  return (
    <>
      <section className="panel">
        <div className="panel-h">
          <h3>Produção prevista × realizada por evento</h3>
        </div>
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                <th>Evento</th>
                <th className="r">Ordens</th>
                <th className="r">Concluídas</th>
                <th className="r">Atrasadas</th>
                <th className="r">Bloqueios</th>
                <th className="r">Realizado / previsto</th>
                <th className="r">Perdas</th>
                <th className="r">Tempo real / estimado</th>
                <th className="r">Ocorrências</th>
                {admin && <th className="r">Custo previsto</th>}
                {admin && <th className="r">Custo realizado</th>}
              </tr>
            </thead>
            <tbody>
              {ind.eventos.map((r) => (
                <tr key={r.eventoId}>
                  <td>
                    {r.codigo} · {r.nome}
                  </td>
                  <td className="r">{r.ordens}</td>
                  <td className="r">{r.concluidas}</td>
                  <td className={`r${r.atrasadas ? " neg" : ""}`}>{r.atrasadas}</td>
                  <td className="r">{r.bloqueios}</td>
                  <td className="r">{pct(r.realizadoPrevisto)}</td>
                  <td className="r">{r.perdas}</td>
                  <td className="r">{r.tempoEstimado ? `${r.tempoReal} / ${r.tempoEstimado} min` : r.tempoReal ? `${r.tempoReal} min` : "—"}</td>
                  <td className="r">{r.ocorrencias}</td>
                  {admin && <td className="r">{brl(r.custoPrevisto)}</td>}
                  {admin && <td className="r">{brl(r.custoRealizado)}</td>}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section className="panel">
        <div className="panel-h">
          <h3>Produção por setor</h3>
        </div>
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                <th>Setor</th>
                <th className="r">Ordens</th>
                <th className="r">Concluídas</th>
                <th className="r">Atrasadas</th>
                <th className="r">Horas registradas</th>
              </tr>
            </thead>
            <tbody>
              {ind.setores.map((s) => (
                <tr key={s.setor}>
                  <td>{s.nome}</td>
                  <td className="r">{s.ordens}</td>
                  <td className="r">{s.concluidas}</td>
                  <td className={`r${s.atrasadas ? " neg" : ""}`}>{s.atrasadas}</td>
                  <td className="r">{Math.round(s.horas * 10) / 10}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      {admin && ind.consumo && ind.consumo.length > 0 && (
        <section className="panel">
          <div className="panel-h">
            <h3>Consumo de insumos pelas ordens</h3>
            <span className="note">15 maiores</span>
          </div>
          <div className="panel-b">
            {ind.consumo.map((c) => (
              <div key={c.nome} className="row static">
                <span className="grow t">{c.nome}</span>
                <b className="num">{qtdU(c.qtd, c.unidade)}</b>
              </div>
            ))}
          </div>
        </section>
      )}
      <p className="note">Realizado / previsto considera as ordens já concluídas. {admin ? `Custo realizado usa a quantidade produzida × custo da ficha. Total previsto: ${brl(sum(ind.eventos, (e) => e.custoPrevisto))}.` : ""}</p>
    </>
  );
}
