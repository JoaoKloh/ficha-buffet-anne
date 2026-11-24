"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { Header } from "@/components/Header";
import { Carregando, Chips, Datebox, Kpi, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import {
  ABAS_CAIXA,
  FILTROS_CAIXA,
  FILTROS_CONTAS,
  FILTROS_UNIDADE,
  type AbaCaixa,
  type FiltroCaixa,
  type FiltroContas,
  type FiltroUnidade,
  type LancamentoListaItemDTO,
} from "@/lib/models/caixa";
import { GRUPOS_DRE, UNIDADES, UN_OPER, SEM_UNIDADE, unCurto, unLabel, type Unidade } from "@/lib/models/dominio";
import { classificarUnidade, telaCaixa, telaContas, telaFluxo, telaSemUnidade, telaTransferencias } from "@/lib/api/gestao/caixa";
import { MESES_L, brl, fdate, fmtN, pct, plural, ymLabel } from "@/lib/utils/numero";
import { addMonths, hoje } from "@/lib/utils/datas";
import { LancRow } from "./LancRow";
import { LancamentoForm, useAbrirLancamento } from "./LancamentoForm";
import { CategoriaForm } from "./CategoriaForm";
import { PagamentoSheet } from "./PagamentoSheet";
import { SaldosForm } from "./SaldosForm";
import { TransferenciaForm } from "./TransferenciaForm";
import { UnPill } from "./UnPill";

const ehAba = (x: string | null): x is AbaCaixa => ABAS_CAIXA.some((a) => a[0] === x);
const ehUnidade = (x: string | null): x is FiltroUnidade => FILTROS_UNIDADE.some((u) => u[0] === x);
const contasTxt = (n: number) => plural(n, "conta", "contas");

/**
 * Caixa: lançamentos por competência, contas a pagar e a receber pelo vencimento,
 * fluxo de caixa com saldos das contas e transferências entre as unidades de negócio.
 * O filtro de unidade vale para todas as abas.
 */
export function CaixaView() {
  const sp = useSearchParams();
  const { abrir } = useSheet();
  const [aba, setAba] = useState<AbaCaixa>(ehAba(sp.get("aba")) ? (sp.get("aba") as AbaCaixa) : "lanc");
  const [unidade, setUnidade] = useState<FiltroUnidade>(ehUnidade(sp.get("unidade")) ? (sp.get("unidade") as FiltroUnidade) : "");
  const presetUn = unidade && unidade !== SEM_UNIDADE ? (unidade as Unidade) : "";
  const novoLanc = () => abrir("Novo lançamento", <LancamentoForm preset={{ unidade: presetUn }} />);
  const novaTransf = () => abrir("Transferência entre unidades", <TransferenciaForm />);

  return (
    <div className="app">
      <Header
        titulo="Caixa"
        sub="Lançamentos, contas a pagar e a receber, fluxo de caixa e as três unidades"
        actions={
          aba === "fluxo" ? (
            <>
              <button type="button" className="btn" onClick={() => abrir("Saldos das contas", <SaldosForm />)}>
                Informar saldos
              </button>
              <button type="button" className="btn primary" onClick={novoLanc}>
                Novo lançamento
              </button>
            </>
          ) : aba === "transf" ? (
            <button type="button" className="btn primary" onClick={novaTransf}>
              Nova transferência
            </button>
          ) : (
            <>
              <button type="button" className="btn" onClick={novaTransf}>
                Transferência entre unidades
              </button>
              <button type="button" className="btn primary" onClick={novoLanc}>
                Novo lançamento
              </button>
            </>
          )
        }
      />
      <Chips opcoes={ABAS_CAIXA} valor={aba} onChange={setAba} rotulo="Seções do caixa" />
      {aba === "lanc" && (unidade === SEM_UNIDADE ? <AbaSemUnidade unidade={unidade} setUnidade={setUnidade} /> : <AbaLancamentos unidade={unidade} setUnidade={setUnidade} />)}
      {aba === "pagar" && <AbaContas receber={false} unidade={unidade} setUnidade={setUnidade} />}
      {aba === "receber" && <AbaContas receber unidade={unidade} setUnidade={setUnidade} />}
      {aba === "fluxo" && <AbaFluxo unidade={unidade} setUnidade={setUnidade} />}
      {aba === "transf" && <AbaTransferencias unidade={unidade} setUnidade={setUnidade} />}
    </div>
  );
}

type FiltroProps = { unidade: FiltroUnidade; setUnidade: (u: FiltroUnidade) => void };

/** Filtro de unidade comum às abas, com o atalho para classificar os lançamentos sem unidade. */
function FiltroUnidadeBar({ unidade, setUnidade, semUnidade }: FiltroProps & { semUnidade: number }) {
  return (
    <div className="toolbar">
      <Sel
        className="sel-inline"
        ariaLabel="Unidade de negócio"
        value={unidade}
        onChange={(x) => setUnidade(x as FiltroUnidade)}
        opcoes={FILTROS_UNIDADE.map(([k, rot]) => [k, k === SEM_UNIDADE && semUnidade ? `${rot} (${semUnidade})` : rot] as const)}
      />
      {semUnidade > 0 && unidade !== SEM_UNIDADE && (
        <button type="button" className="btn sm" onClick={() => setUnidade(SEM_UNIDADE)}>
          {plural(semUnidade, "lançamento", "lançamentos")} sem unidade: classificar
        </button>
      )}
    </div>
  );
}

/* --------------------------------- Lançamentos -------------------------------- */

function AbaLancamentos({ unidade, setUnidade }: FiltroProps) {
  const { abrir } = useSheet();
  const abrirLanc = useAbrirLancamento();
  const [mes, setMes] = useState(hoje().slice(0, 7));
  const [filtro, setFiltro] = useState<FiltroCaixa>("todos");
  const { dados, erro } = useDados(() => telaCaixa(mes, filtro, unidade), [mes, filtro, unidade]);
  const k = dados?.kpis;
  return (
    <>
      <FiltroUnidadeBar unidade={unidade} setUnidade={setUnidade} semUnidade={dados?.semUnidade ?? 0} />
      <div className="toolbar between">
        <div className="monthnav">
          <button type="button" className="btn sm" aria-label="Mês anterior" onClick={() => setMes(addMonths(mes + "-01", -1).slice(0, 7))}>
            ‹
          </button>
          <b>{ymLabel(mes)}</b>
          <button type="button" className="btn sm" aria-label="Próximo mês" onClick={() => setMes(addMonths(mes + "-01", 1).slice(0, 7))}>
            ›
          </button>
        </div>
        <Chips opcoes={FILTROS_CAIXA} valor={filtro} onChange={setFiltro} rotulo="Filtro de lançamentos" />
      </div>
      {k && (
        <div className="kpis">
          <Kpi k="Receitas do mês" v={brl(k.receitas)} h={k.aReceber ? `${brl(k.aReceber)} ainda a receber` : "tudo recebido"} />
          <Kpi k="Custos e despesas" v={brl(k.despesas)} h={k.aPagar ? `${brl(k.aPagar)} ainda a pagar` : "tudo pago"} />
          <Kpi k="Resultado" v={brl(k.resultado)} negativo={k.resultado < 0} h={k.margem != null ? `margem ${pct(k.margem)}` : "—"} />
          <Kpi k="Lançamentos" v={String(k.totalLancamentos)} h={unidade ? unLabel(unidade) : "todas as unidades"} />
        </div>
      )}
      <p className="note">
        Competência: receitas e custos de evento contam no mês do evento; os demais, na competência lançada. O que entrou e saiu do banco em cada dia está em Fluxo de caixa.
      </p>
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.lancamentos.length ? (
            <Vazio titulo={`Nenhum lançamento em ${ymLabel(mes)}`}>
              Parcelas de contrato entram sozinhas no mês do evento. Custos e despesas fixas você lança em Novo lançamento.
            </Vazio>
          ) : (
            dados.lancamentos.map((l) => <LancRow key={l.id} l={l} onAbrir={() => abrirLanc(l)} />)
          )}
        </div>
      </section>
      {dados && (
        <details className="panel">
          <summary className="panel-h" style={{ cursor: "pointer", listStyle: "none" }}>
            <h3>Categorias da DRE</h3>
            <span className="note">{dados.categorias.length} categorias</span>
          </summary>
          <div className="panel-b">
            {!dados.categorias.length && <Vazio titulo="Nenhuma categoria">Crie as linhas da DRE para classificar os lançamentos.</Vazio>}
            {GRUPOS_DRE.map(([g, rot]) => {
              const cs = dados.categorias.filter((c) => c.grupo === g);
              if (!cs.length) return null;
              return (
                <div key={g}>
                  <div className="monthhead">{rot}</div>
                  {cs.map((c) => (
                    <button
                      key={c.id}
                      type="button"
                      className="row"
                      onClick={() => abrir("Editar categoria", <CategoriaForm categoria={c} usada={c.totalLancamentos > 0} proximaOrdem={dados.categorias.length + 1} />)}
                    >
                      <span className="grow t">{c.nome}</span>
                      <span className="m">{c.totalLancamentos} lanç.</span>
                    </button>
                  ))}
                </div>
              );
            })}
            <div style={{ padding: "12px 18px", borderTop: "1px solid var(--line)" }}>
              <button type="button" className="btn sm" onClick={() => abrir("Nova categoria", <CategoriaForm proximaOrdem={dados.categorias.length + 1} />)}>
                Nova categoria
              </button>
            </div>
          </div>
        </details>
      )}
    </>
  );
}

/* --------------------------------- Sem unidade -------------------------------- */

function AbaSemUnidade({ unidade, setUnidade }: FiltroProps) {
  const abrirLanc = useAbrirLancamento();
  const { recarregar } = useSheet();
  const { showToast } = useToast();
  const { dados, erro } = useDados(telaSemUnidade);
  const [todos, setTodos] = useState<Unidade | "">("");
  const [ocupado, setOcupado] = useState(false);
  async function classificar(ids: number[], u: Unidade) {
    setOcupado(true);
    try {
      const r = await classificarUnidade({ unidade: u, ids });
      showToast(ids.length > 1 ? `${plural(r.classificados, "lançamento", "lançamentos")} em ${unCurto(u)}` : `Em ${unCurto(u)}`);
      recarregar();
    } catch (e) {
      showToast(msgErro(e), "error");
    } finally {
      setOcupado(false);
    }
  }
  const ls = dados?.lancamentos ?? [];
  return (
    <>
      <FiltroUnidadeBar unidade={unidade} setUnidade={setUnidade} semUnidade={dados?.total ?? 0} />
      <div className="summary">
        <span>
          <b>{plural(dados?.total ?? 0, "lançamento", "lançamentos")} sem unidade de negócio.</b> Escolha a unidade em cada linha (salva na hora) ou aplique uma só a
          todos os listados. Compras de insumos usados pelas três unidades vão em <b>Estrutura compartilhada</b>, que é rateada na DRE.
        </span>
      </div>
      {!dados ? (
        <section className="panel">
          <Carregando erro={erro} />
        </section>
      ) : !ls.length ? (
        <section className="panel">
          <Vazio titulo="Tudo classificado">Cada lançamento novo já pede a unidade.</Vazio>
        </section>
      ) : (
        <>
          <div className="toolbar">
            <label className="fator">
              <span className="m">Aplicar a todos os {ls.length}</span>
              <Sel className="sel-inline" ariaLabel="Unidade para todos" value={todos} onChange={(x) => setTodos(x as Unidade)} opcoes={UNIDADES} vazio="Escolha…" />
            </label>
            <button type="button" className="btn sm primary" disabled={!todos || ocupado} onClick={() => todos && classificar(ls.map((l) => l.id), todos)}>
              Aplicar
            </button>
          </div>
          <section className="panel">
            <div className="panel-b">
              {ls.map((l) => {
                const r = l.grupo === "receita";
                return (
                  <div key={l.id} className="row static" style={{ flexWrap: "wrap" }}>
                    <Datebox iso={l.competencia} />
                    <button type="button" className="grow linkish" onClick={() => abrirLanc(l)} style={{ minWidth: 160 }}>
                      <span className="t">{l.descricao || "(sem descrição)"}</span>
                      <span className="m">{[l.categoriaNome ?? "Sem categoria", l.fornecedor].filter(Boolean).join(" · ")}</span>
                    </button>
                    <Sel
                      className="sel-inline"
                      ariaLabel={`Unidade de ${l.descricao}`}
                      value=""
                      disabled={ocupado}
                      onChange={(x) => x && classificar([l.id], x as Unidade)}
                      opcoes={UNIDADES.map(([k]) => [k, unCurto(k)] as const)}
                      vazio="Unidade…"
                    />
                    <span className={`amt${r ? " pos" : ""}`}>
                      {r ? "+ " : "− "}
                      {brl(l.valor)}
                    </span>
                  </div>
                );
              })}
            </div>
          </section>
          {dados.total > ls.length && <p className="note">Mostrando {ls.length} de {dados.total}. Os demais aparecem conforme estes forem classificados.</p>}
        </>
      )}
    </>
  );
}

/* ------------------------------ Contas a pagar/receber ------------------------------ */

function AbaContas({ receber, unidade, setUnidade }: FiltroProps & { receber: boolean }) {
  const { abrir } = useSheet();
  const abrirLanc = useAbrirLancamento();
  const [filtro, setFiltro] = useState<FiltroContas>("abertas");
  const tipo = receber ? "receber" : "pagar";
  const { dados, erro } = useDados(() => telaContas(tipo, filtro, unidade), [tipo, filtro, unidade]);
  const pagar = (l: LancamentoListaItemDTO) => abrir(receber ? "Recebimento" : "Pagamento", <PagamentoSheet id={l.id} onEditar={() => abrirLanc(l)} />);
  return (
    <>
      <FiltroUnidadeBar unidade={unidade} setUnidade={setUnidade} semUnidade={dados?.semUnidade ?? 0} />
      {dados && (
        <div className="kpis">
          <Kpi k="Em aberto" v={brl(dados.emAberto.valor)} h={`${contasTxt(dados.emAberto.n)} a ${tipo}`} />
          <Kpi k="Vencido" v={brl(dados.vencido.valor)} negativo={dados.vencido.n > 0} h={dados.vencido.n ? contasTxt(dados.vencido.n) : "nada vencido"} />
          <Kpi k="Vence em 7 dias" v={brl(dados.seteDias.valor)} h={contasTxt(dados.seteDias.n)} />
          <Kpi
            k={receber ? "Recebido em parte" : "Pago em parte"}
            v={brl(dados.parcial.pago)}
            h={dados.parcial.n ? `${dados.parcial.n} com saldo de ${brl(dados.parcial.saldo)}` : "nenhuma"}
          />
        </div>
      )}
      <Chips opcoes={FILTROS_CONTAS} valor={filtro} onChange={setFiltro} rotulo={`Filtro de contas a ${tipo}`} />
      <section className="panel">
        {!dados ? (
          <Carregando erro={erro} />
        ) : !dados.contas.length ? (
          <Vazio titulo="Nada neste filtro">
            {receber
              ? "Parcelas de contrato, extras e outras receitas previstas aparecem aqui pelo vencimento."
              : "Boletos de notas fiscais e despesas previstas aparecem aqui pelo vencimento."}
          </Vazio>
        ) : (
          <div className="tablewrap">
            <table>
              <thead>
                <tr>
                  <th>Vencimento</th>
                  <th>Descrição</th>
                  <th className="r">Valor</th>
                  <th className="r">{receber ? "Recebido" : "Pago"}</th>
                  <th className="r">Saldo</th>
                  <th>Situação</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {dados.contas.map((l) => (
                  <tr key={l.id}>
                    <td>{fdate(l.vencimentoEfetivo)}</td>
                    <td style={{ whiteSpace: "normal", minWidth: 180 }}>
                      <button type="button" className="linkish" onClick={() => abrirLanc(l)}>
                        <b>{l.descricao || "(sem descrição)"}</b>
                        <br />
                        <span className="note">{[l.parte, l.categoriaNome].filter(Boolean).join(" · ")}</span>
                      </button>{" "}
                      <UnPill u={l.unidadeEfetiva} />
                    </td>
                    <td className="r">{brl(l.valor)}</td>
                    <td className="r">{l.pago ? brl(l.pago) : "—"}</td>
                    <td className="r">
                      <b>{brl(l.saldo)}</b>
                    </td>
                    <td>
                      <Pill tom={l.situacaoTom}>{l.situacao}</Pill>
                    </td>
                    <td>
                      {l.status === "realizado" ? (
                        <button type="button" className="btn sm ghost" onClick={() => pagar(l)}>
                          Ver
                        </button>
                      ) : (
                        <button type="button" className="btn sm" onClick={() => pagar(l)}>
                          {receber ? "Receber" : "Pagar"}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <p className="note">
        Saldo = valor − o que já foi {receber ? "recebido" : "pago"}. Toque em {receber ? "Receber" : "Pagar"} para registrar o valor inteiro ou só uma parte; cada parte
        fica no histórico com data, conta e quem registrou.{receber ? " Parcelas de eventos cancelados ou perdidos não aparecem." : ""}
      </p>
    </>
  );
}

/* --------------------------------- Fluxo de caixa -------------------------------- */

function AbaFluxo({ unidade, setUnidade }: FiltroProps) {
  const { abrir } = useSheet();
  const { dados, erro } = useDados(() => telaFluxo(unidade), [unidade]);
  if (!dados)
    return (
      <>
        <FiltroUnidadeBar unidade={unidade} setUnidade={setUnidade} semUnidade={0} />
        <section className="panel">
          <Carregando erro={erro} />
        </section>
      </>
    );
  const mesL = MESES_L[Number(dados.mes.slice(5, 7)) - 1];
  const saldos = () => abrir("Saldos das contas", <SaldosForm contas={dados.contas} data={dados.saldosData} />);
  return (
    <>
      <FiltroUnidadeBar unidade={unidade} setUnidade={setUnidade} semUnidade={dados.semUnidade} />
      <div className="kpis">
        <Kpi
          k="Saldo em conta"
          v={dados.temSaldo ? brl(dados.saldoTotal) : "—"}
          negativo={dados.saldoTotal < 0}
          h={dados.temSaldo ? "saldo informado + o que entrou e saiu depois" : "informe o saldo de cada conta"}
        />
        <Kpi k="Recebido no mês" v={brl(dados.recebidoMes)} h={`pagamentos de clientes em ${mesL}`} />
        <Kpi k="Pago no mês" v={brl(dados.pagoMes)} h={`saídas registradas em ${mesL}`} />
        <Kpi
          k="Saldo projetado em 30 dias"
          v={dados.projetado30 != null ? brl(dados.projetado30) : "—"}
          negativo={(dados.projetado30 ?? 0) < 0}
          h={`${brl(dados.entradas30)} a entrar · ${brl(dados.saidas30)} a sair`}
        />
      </div>
      {!dados.temSaldo && (
        <div className="banner">
          Informe o saldo atual de cada conta em <b>Informar saldos</b>. A partir daí o sistema soma os recebimentos e subtrai os pagamentos registrados, e projeta o
          saldo pelos vencimentos.
        </div>
      )}
      <section className="panel">
        <div className="panel-h">
          <h3>Saldos por conta</h3>
          <button type="button" className="btn sm" onClick={saldos}>
            Informar saldos
          </button>
        </div>
        <div className="panel-b">
          {dados.contas.map((c) => (
            <div key={c.conta} className="row static">
              <span className="grow">
                <span className="t">{c.rotulo}</span>
                <span className="m">{c.informadoEm ? `informado ${brl(c.informado)} em ${fdate(c.informadoEm)}` : "saldo ainda não informado"}</span>
              </span>
              <span className={`amt${(c.saldo ?? 0) < 0 ? " neg" : ""}`}>{c.saldo == null ? "—" : brl(c.saldo)}</span>
            </div>
          ))}
        </div>
      </section>
      <section className="panel">
        <div className="panel-h">
          <h3>Fluxo projetado</h3>
          <span className="note">pelo vencimento do que está em aberto{unidade ? ` · ${unidade === SEM_UNIDADE ? "Sem unidade" : unLabel(unidade)}` : ""}</span>
        </div>
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                <th>Período</th>
                <th className="r">Entradas previstas</th>
                <th className="r">Saídas previstas</th>
                <th className="r">Líquido</th>
                <th className="r">Saldo projetado</th>
              </tr>
            </thead>
            <tbody>
              {dados.linhas.map((l) => (
                <tr key={l.rotulo} className={l.vencidos ? "sum" : ""}>
                  <td>{l.rotulo}</td>
                  <td className="r pos">{l.entradas ? brl(l.entradas) : "—"}</td>
                  <td className="r neg">{l.saidas ? brl(l.saidas) : "—"}</td>
                  <td className={`r${l.liquido < 0 ? " neg" : ""}`}>{brl(l.liquido)}</td>
                  <td className="r">
                    <b className={(l.saldo ?? 0) < 0 ? "neg" : ""}>{l.saldo == null ? "—" : brl(l.saldo)}</b>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <p className="note">
        Entradas e saídas previstas usam o saldo ainda em aberto de cada conta (o que já foi pago em parte não entra de novo). O saldo em conta é de todas as unidades
        juntas, porque o banco é um só; o filtro de unidade muda só as previsões.
      </p>
    </>
  );
}

/* --------------------------------- Transferências -------------------------------- */

function AbaTransferencias({ unidade, setUnidade }: FiltroProps) {
  const { abrir } = useSheet();
  const { dados, erro } = useDados(() => telaTransferencias(unidade), [unidade]);
  return (
    <>
      <FiltroUnidadeBar unidade={unidade} setUnidade={setUnidade} semUnidade={dados?.semUnidade ?? 0} />
      <p className="note">
        Quando uma unidade usa produto ou insumo comprado por outra (o restaurante usa um molho da fábrica do buffet, a loja usa chocolate do estoque geral), registre
        aqui. O custo sai da unidade de origem e entra na de destino, com quantidade, data e responsável. Não mexe no caixa nem no resultado consolidado, só na DRE de
        cada unidade.
      </p>
      {dados && (
        <div className="kpis">
          {UN_OPER.map((u) => {
            const p = dados.porUnidade.find((x) => x.unidade === u);
            return <Kpi key={u} k={`${unCurto(u)} no mês`} v={brl(p?.liquido ?? 0)} h={`recebeu ${brl(p?.recebeu ?? 0)} · enviou ${brl(p?.enviou ?? 0)}`} />;
          })}
          <Kpi k="Transferências" v={String(dados.total)} h={`${dados.doMes} neste mês`} />
        </div>
      )}
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.transferencias.length ? (
            <Vazio titulo="Nenhuma transferência">Registre quando uma unidade usar produto ou insumo de outra.</Vazio>
          ) : (
            dados.transferencias.map((l) => (
              <button key={l.id} type="button" className="row" onClick={() => abrir("Transferência", <TransferenciaForm transferencia={l} />)}>
                <Datebox iso={l.dataCompetencia} />
                <span className="grow">
                  <span className="t">
                    {unCurto(l.unidadeOrigem)} → {unCurto(l.unidadeDestino)}
                  </span>
                  <span className="m">{[l.item || l.descricao, l.qtd ? `${fmtN(l.qtd)} ${l.unid ?? ""}`.trim() : null, l.responsavel].filter(Boolean).join(" · ")}</span>
                </span>
                <span className="amt">{brl(l.valor)}</span>
              </button>
            ))
          )}
        </div>
      </section>
    </>
  );
}
