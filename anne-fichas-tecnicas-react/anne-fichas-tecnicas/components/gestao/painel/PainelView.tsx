"use client";

import Link from "next/link";
import { Header } from "@/components/Header";
import { Carregando, Kpi, Vazio } from "@/components/ui/Gestao";
import { ICONES } from "@/components/ui/Icones";
import { useDados, useSheet } from "@/components/ui/Sheet";
import type { AlertaDTO, PainelGraficosDTO, PainelOperacaoDTO, PainelUnidadesDTO } from "@/lib/models/painel";
import { UN_OPER, unCurto } from "@/lib/models/dominio";
import { UnPill } from "../caixa/UnPill";
import { resumoPainel } from "@/lib/api/gestao/painel";
import { brl, capitalizar, fdate, fmtN, mesCurto, pct, plural, ymLabel } from "@/lib/utils/numero";
import { EvRow } from "../eventos/EvRow";
import { EventoForm } from "../eventos/EventoForm";
import { EventoSheet } from "../eventos/EventoSheet";
import { stTone } from "../eventos/status";
import { BackupPainel } from "./Backup";
import { COR, GraficoBarrasH, GraficoMeses, GraficoRosca } from "./Graficos";

/** Painel da gestão: o mês em números, o que pede atenção e os próximos eventos. */
export function PainelView() {
  const { abrir } = useSheet();
  const { dados, erro } = useDados(resumoPainel);
  const backup = () => abrir("Backup e restauração", <BackupPainel />);
  const k = dados?.kpis;

  return (
    <div className="app">
      <Header
        sub={dados ? `${capitalizar(ymLabel(dados.mes))} · hoje ${fdate(dados.hoje)}` : undefined}
        actions={
          <>
            <button type="button" className="btn" onClick={backup}>
              Salvar backup
            </button>
            <button type="button" className="btn primary" onClick={() => abrir("Novo evento", <EventoForm />)}>
              Novo evento
            </button>
          </>
        }
      />
      {!dados || !k ? (
        <section className="panel">
          <Carregando erro={erro} />
        </section>
      ) : (
        <>
          <div className="kpis k5">
            <Kpi k="Faturamento do mês" v={brl(k.faturamento)} h="competência: eventos e vendas deste mês" />
            <Kpi k="Recebido no mês" v={brl(k.recebido)} h="caixa: dinheiro que entrou" />
            <Kpi
              k="A receber em 30 dias"
              v={brl(k.aReceber)}
              h={
                k.receberVencidas ? (
                  <span className="neg">
                    {plural(k.receberVencidas, "vencida", "vencidas")} · {brl(k.receberVencido)}
                  </span>
                ) : (
                  plural(k.contasReceber, "conta", "contas")
                )
              }
            />
            <Kpi
              k="A pagar em 7 dias"
              v={brl(k.aPagar)}
              h={
                k.pagarVencidas ? (
                  <span className="neg">
                    {plural(k.pagarVencidas, "vencida", "vencidas")} · {brl(k.pagarVencido)}
                  </span>
                ) : (
                  plural(k.contasPagar, "conta", "contas")
                )
              }
            />
            <Kpi k="Resultado do mês" v={brl(k.resultado)} negativo={k.resultado < 0} h={k.margem != null && k.faturamento ? `margem ${pct(k.margem)}` : "pela DRE do mês"} />
          </div>
          <div className="grid2">
            <section className="panel">
              <div className="panel-h">
                <h3>Atenção</h3>
                <span className={`pill ${dados.alertas.length ? "warn" : "good"}`}>{dados.alertas.length}</span>
              </div>
              <div className="panel-b">
                {dados.alertas.length ? (
                  dados.alertas.map((a, i) => <Alerta key={i} a={a} onBackup={backup} />)
                ) : (
                  <Vazio titulo="Nada pendente">Prazos de cardápio, parcelas vencidas, contas a pagar e a receber e eventos por acertar aparecem aqui.</Vazio>
                )}
              </div>
            </section>
            <section className="panel">
              <div className="panel-h">
                <h3>Próximos eventos</h3>
                <Link className="btn sm ghost" href="/eventos">
                  Ver todos
                </Link>
              </div>
              <div className="panel-b">
                {dados.proximos.length ? (
                  dados.proximos.map((e) => <EvRow key={e.id} e={e} onClick={() => abrir(e.nome, <EventoSheet id={e.id} />)} />)
                ) : (
                  <Vazio titulo="Nenhum evento agendado">Cadastre o primeiro em Novo evento.</Vazio>
                )}
              </div>
            </section>
          </div>
          {dados.operacao && <Operacao o={dados.operacao} />}
          <Unidades u={dados.unidades} mes={dados.mes} />
          <Graficos g={dados.graficos} />
        </>
      )}
    </div>
  );
}

/** Resultado do mês por unidade de negócio, com o CMV e a margem de cada uma. */
/** Faixa de operação: cada KPI leva à tela onde se resolve. */
function Operacao({ o }: { o: PainelOperacaoDTO }) {
  const item = (href: string, k: string, v: string, h: string, neg?: boolean) => (
    <Link className="kpilink" href={href}>
      <Kpi k={k} v={v} h={h} negativo={neg} />
    </Link>
  );
  return (
    <div className="kpis k6">
      {item("/estoque?aba=saldo", "Estoque crítico", String(o.estoqueCritico.n), o.estoqueCritico.n ? `${o.estoqueCritico.nomes.slice(0, 2).join(", ")}${o.estoqueCritico.n > 2 ? "…" : ""}` : "nada abaixo do mínimo", o.estoqueCritico.n > 0)}
      {item("/estoque?aba=validades", "Vencendo em 7 dias", String(o.vencendo.n), o.vencendo.vencidos ? plural(o.vencendo.vencidos, "já vencido", "já vencidos") : "insumos e pré-preparos", o.vencendo.vencidos > 0)}
      {item("/cozinha?aba=lider", "Produções atrasadas", String(o.producoesAtrasadas.n), o.producoesAtrasadas.eventos ? `em ${plural(o.producoesAtrasadas.eventos, "evento", "eventos")}` : "cozinha em dia", o.producoesAtrasadas.n > 0)}
      {item("/estoque?aba=compras", "Compras pendentes", String(o.comprasPendentes.n), o.comprasPendentes.n ? `${brl(o.comprasPendentes.valor)} p/ 7 dias` : "nada a pedir")}
      {item("/eventos", "Eventos com pendências", String(o.eventosComPendencias), "próximos 30 dias ou desvio de custo", o.eventosComPendencias > 0)}
      {item("/dre", "CMV e margem do mês", pct(o.cmv), `meta ${pct(o.metaCmv)} · margem ${pct(o.margem)}`)}
    </div>
  );
}

function Unidades({ u, mes }: { u: PainelUnidadesDTO; mes: string }) {
  return (
    <section className="panel">
      <div className="panel-h">
        <h3>Resultado por unidade · {ymLabel(mes)}</h3>
        <Link className="btn sm ghost" href="/dre">
          Ver DRE por unidade
        </Link>
      </div>
      <div className="tablewrap">
        <table>
          <thead>
            <tr>
              <th>Unidade</th>
              <th className="r">Faturamento</th>
              <th className="r">Custos diretos (CMV)</th>
              <th className="r">% CMV</th>
              <th className="r">Margem de contribuição</th>
              <th className="r">Resultado</th>
              <th className="r">Margem</th>
            </tr>
          </thead>
          <tbody>
            {u.linhas.map((c) => (
              <tr key={c.unidade}>
                <td>
                  <UnPill u={c.unidade} />
                </td>
                <td className="r">{brl(c.faturamento)}</td>
                <td className="r">{brl(c.cmv)}</td>
                <td className="r">{pct(c.pctCmv)}</td>
                <td className="r">{brl(c.margemContribuicao)}</td>
                <td className="r">
                  <b className={c.resultado < 0 ? "neg" : ""}>{brl(c.resultado)}</b>
                </td>
                <td className="r">{pct(c.margem)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {u.semUnidadeMes ? (
        <p className="note" style={{ padding: "0 18px 12px" }}>
          {plural(u.semUnidadeMes, "lançamento", "lançamentos")} deste mês sem unidade não {u.semUnidadeMes > 1 ? "entram" : "entra"} aqui.{" "}
          <Link className="btn sm" href="/caixa?aba=lanc&unidade=_sem">
            Classificar
          </Link>
        </p>
      ) : (
        <p className="note" style={{ padding: "0 18px 12px" }}>
          Competência, com o previsto do mês. A estrutura compartilhada entra pelo rateio ({UN_OPER.map((x) => `${unCurto(x)} ${fmtN(Math.round(u.rateio[x]))}%`).join(" · ")}).
        </p>
      )}
    </section>
  );
}

function Alerta({ a, onBackup }: { a: AlertaDTO; onBackup: () => void }) {
  const { abrir } = useSheet();
  const corpo = (
    <>
      <span className="dot" />
      <span className="grow">{a.partes.map((p, i) => (typeof p === "string" ? p : <b key={i}>{p.b}</b>))}</span>
    </>
  );
  const cls = `alert ${a.tom}`;
  if (a.destino.tipo === "href")
    return (
      <Link className={cls} href={a.destino.href}>
        {corpo}
      </Link>
    );
  const d = a.destino;
  return (
    <button type="button" className={cls} onClick={() => (d.tipo === "evento" ? abrir("Evento", <EventoSheet id={d.id} />) : onBackup())}>
      {corpo}
    </button>
  );
}

function Cartao({ titulo, sub, rodape, vazio, alto, extra, children }: { titulo: string; sub: React.ReactNode; rodape: React.ReactNode; vazio?: string | false; alto?: boolean; extra?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="ccard">
      <div>
        <h4>{titulo}</h4>
        <p className="cs">{sub}</p>
      </div>
      {extra}
      <div className={`cbox${alto ? " tall" : ""}`}>{vazio ? <div className="cempty">{vazio}</div> : children}</div>
      <div className="cft">
        {ICONES.clock}
        <span>{rodape}</span>
      </div>
    </div>
  );
}

const soma = (a: number[]) => a.reduce((s, x) => s + x, 0);
const COR_ETAPA: Record<string, string> = { bad: COR.rosa, good: COR.verde, brass: COR.dourado, deg: COR.ameixa, fech: COR.azul, accent: COR.verde };

function Graficos({ g }: { g: PainelGraficosDTO }) {
  const rot = g.meses.map(mesCurto);
  const mi = g.mesAtual;
  const totRec = soma(g.receitas);
  const totDesp = soma(g.despesas);
  const temLanc = totRec > 0 || totDesp > 0;
  const futRec = soma(g.receitas.slice(mi + 1));
  const rm = g.resultado[mi] ?? 0;
  const nEv = soma(g.eventos);
  const nF = soma(g.funil.map((x) => x.n));
  const tD = soma(g.despesasCategoria.map((x) => x.valor));
  return (
    <>
      <div className="charts c3">
        <Cartao
          titulo="Receitas e despesas"
          sub={`Por mês de competência, de ${rot[0]} a ${rot[rot.length - 1]}`}
          rodape={futRec ? `${brl(futRec)} em receitas já contratadas nos próximos 6 meses` : "Sem receitas lançadas para os próximos meses"}
          vazio={!temLanc && "Nenhum lançamento neste período. Os valores aparecem conforme você lança receitas e despesas."}
          extra={
            <div className="legend">
              <span>
                <i style={{ background: COR.verde }} />
                Receitas
              </span>
              <span>
                <i style={{ background: COR.rosa }} />
                Despesas
              </span>
            </div>
          }
        >
          <GraficoMeses
            titulo="Receitas e despesas"
            tipo="barras"
            dinheiro
            atual={mi}
            rotulos={rot}
            series={[
              { rotulo: "Receitas", valores: g.receitas, cor: COR.verde },
              { rotulo: "Despesas", valores: g.despesas, cor: COR.rosa },
            ]}
          />
        </Cartao>
        <Cartao
          titulo="Resultado por mês"
          sub={
            <>
              Receitas menos despesas · {rot[mi]}: <b className={rm < 0 ? "neg" : "pos"}>{brl(rm)}</b>
            </>
          }
          rodape={`Acumulado dos 12 meses: ${brl(soma(g.resultado))}`}
          vazio={!temLanc && "Sem lançamentos para calcular o resultado."}
        >
          <GraficoMeses titulo="Resultado por mês" tipo="linha" dinheiro atual={mi} rotulos={rot} series={[{ rotulo: "Resultado", valores: g.resultado, cor: COR.dourado }]} />
        </Cartao>
        <Cartao
          titulo="Eventos por mês"
          sub="Eventos ativos nos próximos 12 meses"
          rodape={`${plural(nEv, "evento", "eventos")} · ${soma(g.convidados).toLocaleString("pt-BR")} convidados previstos`}
          vazio={!nEv && "Nenhum evento com data nos próximos 12 meses."}
        >
          <GraficoMeses titulo="Eventos por mês" tipo="barras" rotulos={g.mesesEventos.map(mesCurto)} series={[{ rotulo: "Eventos", valores: g.eventos, cor: COR.azul }]} />
        </Cartao>
      </div>
      <div className="charts c2">
        <Cartao
          alto
          titulo="Funil de eventos"
          sub="Quantos eventos estão em cada etapa"
          rodape={`${plural(nF, "evento cadastrado", "eventos cadastrados")} · ${g.contratadosOuAdiante} contratados ou adiante`}
          vazio={!nF && "Nenhum evento cadastrado ainda."}
        >
          <GraficoBarrasH titulo="Funil de eventos" itens={g.funil.map((x) => ({ rotulo: x.rotulo, valor: x.n, cor: COR_ETAPA[stTone(x.status)] ?? COR.neutro }))} />
        </Cartao>
        <Cartao
          alto
          titulo="Despesas por categoria"
          sub={`De ${mesCurto(g.despesasDe)} a ${rot[mi]} · total ${brl(tD)}`}
          rodape={g.despesasCategoria.length ? `Maior gasto: ${g.despesasCategoria[0]!.nome} (${pct(g.despesasCategoria[0]!.valor / (tD || 1))})` : "Sem despesas no período"}
          vazio={!tD && "Nenhuma despesa lançada nos últimos 4 meses."}
        >
          <GraficoRosca titulo="Despesas por categoria" itens={g.despesasCategoria} />
        </Cartao>
      </div>
    </>
  );
}
