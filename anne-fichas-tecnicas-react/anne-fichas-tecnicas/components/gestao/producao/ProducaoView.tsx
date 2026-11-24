"use client";

import { useState } from "react";
import { Header } from "@/components/Header";
import { Carregando, Chips, Datebox, FormSheet, Fld, Kpi, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { CATEGORIAS_PREPARACAO, SECOES_CARTA, STATUS_PLANO, UNIDADES_PLANO, rotulo, type StatusPlano, type UnidadePlano } from "@/lib/models/dominio";
import type { LinhaPlanoDTO, PlanoDetalheResponseDTO, PlanoListaItemDTO, PlanoProducaoResponseDTO } from "@/lib/models/planoProducao";
import {
  adicionarItensPlano,
  ajustarItemPlano,
  atualizarPlano,
  calcularPelaCarta,
  concluirItemPlano,
  criarPlano,
  desfazerItemPlano,
  detalhePlano,
  excluirPlano,
  removerItemPlano,
  telaPlanos,
} from "@/lib/api/gestao/planosProducao";
import { listarPreparacoes, telaPratosVenda } from "@/lib/api/gestao/fichas";
import { brl, dec, fdate, fmtN, numIn, qtdU } from "@/lib/utils/numero";
import { addDays, hoje } from "@/lib/utils/datas";
import { LoteSheet } from "../estoque/Lotes";
import { imprimirPlano } from "./imprimir";

/**
 * Produção do Restaurante ANNE e da Chocolates Marie & Anne (protótipo: vProducao),
 * separada dos eventos do buffet. Cada item concluído vira um lote em Estoque › Pré-preparos.
 */

const statusTom = (s: StatusPlano) => (s === "concluido" ? "good" : s === "em_producao" ? "accent" : "");
const periodo = (p: { dataInicio: string; dataFim: string }) => (p.dataInicio ? `${fdate(p.dataInicio)}${p.dataFim && p.dataFim !== p.dataInicio ? ` a ${fdate(p.dataFim)}` : ""}` : "sem data");
const ABAS = [
  ["lista", "Lista de produção"],
  ["insumos", "Insumos por item"],
  ["compras", "Lista de compras"],
] as const;
type Aba = (typeof ABAS)[number][0];

export function ProducaoView() {
  const [unidade, setUnidade] = useState<UnidadePlano>("restaurante");
  const [sel, setSel] = useState<number | null>(null);
  const { abrir } = useSheet();
  return (
    <div className="app">
      <Header
        sub="Restaurante e fábrica de chocolates · separado dos eventos do buffet"
        actions={
          <button type="button" className="btn primary" onClick={() => abrir("Novo plano de produção", <PlanoForm unidade={unidade} onCriado={setSel} />)}>
            Novo plano
          </button>
        }
      />
      <Chips
        opcoes={UNIDADES_PLANO}
        valor={unidade}
        onChange={(u) => {
          setUnidade(u);
          setSel(null);
        }}
        rotulo="Unidade"
      />
      {sel == null ? <ListaPlanos unidade={unidade} onAbrir={setSel} /> : <Plano id={sel} voltar={() => setSel(null)} />}
    </div>
  );
}

function ListaPlanos({ unidade, onAbrir }: { unidade: UnidadePlano; onAbrir: (id: number) => void }) {
  const { dados, erro } = useDados(() => telaPlanos(unidade), [unidade]);
  if (!dados) return <Carregando erro={erro} />;
  const k = dados.kpis;
  return (
    <>
      <div className="kpis">
        <Kpi k="Planos abertos" v={String(k.abertos)} h={k.abertos ? "em andamento ou planejados" : "nenhum"} />
        <Kpi k="Itens a produzir" v={String(k.itensAProduzir)} h="nos planos abertos" />
        <Kpi k="Concluídos" v={String(k.concluidos)} h="planos fechados" />
        <Kpi k="Pré-preparos prontos" v={String(k.prePreparosProntos)} h="lotes com saldo" />
      </div>
      <section className="panel">
        <div className="panel-h">
          <h3>Planos de produção · {unidade === "chocolates" ? "Chocolates" : "Restaurante"}</h3>
        </div>
        <div className="panel-b">
          {dados.planos.length ? (
            dados.planos.map((p: PlanoListaItemDTO) => (
              <button key={p.id} type="button" className="row" onClick={() => onAbrir(p.id)}>
                <Datebox iso={p.dataInicio} />
                <span className="grow">
                  <span className="t">{p.titulo || "Plano de produção"}</span>
                  <span className="m">
                    {periodo(p)} · {p.itens} ite{p.itens === 1 ? "m" : "ns"}
                    {p.itens ? ` · ${p.feitos} feito${p.feitos === 1 ? "" : "s"}` : ""}
                  </span>
                </span>
                <Pill tom={statusTom(p.status)}>{rotulo(STATUS_PLANO, p.status)}</Pill>
              </button>
            ))
          ) : (
            <Vazio titulo="Nenhum plano ainda">Crie um plano para o dia ou a semana, marque as fichas que vão ser produzidas e as quantidades. O sistema monta a lista de produção, os insumos e a lista de compras.</Vazio>
          )}
        </div>
      </section>
      <p className="note">Cada item concluído vira um lote em Estoque › Pré-preparos, com validade da ficha e baixa dos insumos no estoque.</p>
    </>
  );
}

function Plano({ id, voltar }: { id: number; voltar: () => void }) {
  const { abrir } = useSheet();
  const { showToast } = useToast();
  const [aba, setAba] = useState<Aba>("lista");
  const { dados, erro } = useDados(() => detalhePlano(id), [id]);
  if (!dados) return <Carregando erro={erro} />;
  const pl = dados.plano;
  const n = dados.linhas.length;
  const f = dados.linhas.filter((l) => l.feito).length;
  function imprimir() {
    try {
      if (imprimirPlano(dados!) === "arquivo") showToast("Arquivo salvo: abra e imprima");
    } catch (e) {
      showToast(msgErro(e, "Não foi possível gerar o arquivo agora."), "error");
    }
  }
  return (
    <>
      <div className="toolbar">
        <button type="button" className="btn sm ghost" onClick={voltar}>
          ← Todos os planos
        </button>
      </div>
      <div className="prev">
        <div style={{ flex: "1 1 260px", minWidth: 0 }}>
          <span className="k">
            {rotulo(UNIDADES_PLANO, pl.unidade)} · {periodo(pl)}
          </span>
          <b>{pl.titulo || "Plano de produção"}</b>
          {pl.obs && (
            <span className="m" style={{ display: "block", whiteSpace: "pre-wrap" }}>
              {pl.obs}
            </span>
          )}
        </div>
        <div style={{ textAlign: "right", flex: "none" }}>
          <span className="k">Concluído</span>
          <b className="num">
            {f}/{n}
          </b>
          <br />
          <span className="m">{rotulo(STATUS_PLANO, pl.status)}</span>
        </div>
      </div>
      <div className="toolbar">
        <button type="button" className="btn sm primary" onClick={() => abrir("Adicionar itens ao plano", <AdicionarItensSheet plano={pl} />, { larga: true })}>
          Adicionar itens
        </button>
        {pl.unidade === "restaurante" && (
          <button type="button" className="btn sm" onClick={() => abrir("Calcular pela carta", <CartaSheet plano={pl} />, { larga: true })}>
            Calcular pela carta
          </button>
        )}
        <button type="button" className="btn sm" onClick={() => abrir("Editar plano", <PlanoForm plano={pl} unidade={pl.unidade} onExcluido={voltar} />)}>
          Editar plano
        </button>
        <span style={{ flex: 1 }} />
        <button type="button" className="btn sm" onClick={imprimir} title="Página A4 para imprimir ou salvar em PDF pelo navegador">
          Imprimir / baixar
        </button>
      </div>
      <Chips opcoes={ABAS} valor={aba} onChange={setAba} rotulo="Seções do plano" />
      {!n ? (
        <section className="panel">
          <Vazio titulo="Plano vazio">Toque em &quot;Adicionar itens&quot; e marque as fichas a produzir{pl.unidade === "restaurante" ? ', ou use "Calcular pela carta" a partir dos pratos previstos' : ""}.</Vazio>
        </section>
      ) : aba === "insumos" ? (
        <AbaInsumosPlano d={dados} />
      ) : aba === "compras" ? (
        <AbaComprasPlano d={dados} />
      ) : (
        <AbaListaPlano d={dados} />
      )}
    </>
  );
}

function AbaListaPlano({ d }: { d: PlanoDetalheResponseDTO }) {
  const { abrir, recarregar } = useSheet();
  const { showToast } = useToast();
  const id = d.plano.id;
  async function acao(fn: () => Promise<unknown>, ok: (r: unknown) => string) {
    try {
      const r = await fn();
      showToast(ok(r));
      recarregar();
    } catch (e) {
      showToast(msgErro(e), "error");
    }
  }
  const grupos = [...CATEGORIAS_PREPARACAO.map(([k]) => k as string), "_"];
  return (
    <>
      <section className="panel">
        <div className="tablewrap">
          <table className="cztable">
            <thead>
              <tr>
                <th>Item</th>
                <th>Praça</th>
                <th className="r">Quantidade</th>
                <th className="r">Receitas</th>
                <th>Ficha</th>
                <th>Situação</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {grupos.map((g) => {
                const ls = d.linhas.filter((l) => (CATEGORIAS_PREPARACAO.some(([c]) => c === l.categoria) ? l.categoria : "_") === g).sort((a, b) => (a.nome ?? "").localeCompare(b.nome ?? "", "pt-BR"));
                if (!ls.length) return null;
                return [
                  <tr key={`g${g}`} className="grp-row">
                    <td colSpan={7}>{g === "_" ? "Outros" : rotulo(CATEGORIAS_PREPARACAO, g)}</td>
                  </tr>,
                  ...ls.map((l) => (
                    <LinhaPlano
                      key={l.k}
                      l={l}
                      onQtd={(q) => acao(() => ajustarItemPlano(id, l.k, q), () => "Quantidade ajustada")}
                      onConcluir={() => acao(() => concluirItemPlano(id, l.k), (r) => `Concluído · lote ${(r as { codigo: string }).codigo} em Pré-preparos`)}
                      onDesfazer={() => acao(() => desfazerItemPlano(id, l.k), () => "Item voltou para a produção · lote apagado")}
                      onRemover={() => acao(() => removerItemPlano(id, l.k), () => "Item tirado do plano")}
                      onLote={() => l.lote && abrir("Lote", <LoteSheet id={l.lote.id} />)}
                    />
                  )),
                ];
              })}
            </tbody>
          </table>
        </div>
      </section>
      <p className="note">
        Quantidade na unidade da ficha (porções, unidades, kg ou litros). &quot;Concluir&quot; registra o lote em Estoque › Pré-preparos com a validade da ficha e baixa os insumos. &quot;Desfazer&quot; apaga o lote, se ele ainda não foi usado.
      </p>
    </>
  );
}

function LinhaPlano({ l, onQtd, onConcluir, onDesfazer, onRemover, onLote }: { l: LinhaPlanoDTO; onQtd: (q: number) => void; onConcluir: () => void; onDesfazer: () => void; onRemover: () => void; onLote: () => void }) {
  const { showToast } = useToast();
  const [q, setQ] = useState(dec(l.qtd));
  const [ocupado, setOcupado] = useState(false);
  const tom = l.fichaStatus === "Completa" ? "good" : l.fichaStatus === "Ficha removida" ? "bad" : "warn";
  const rot = l.fichaStatus === "Incompleta" ? "Sem ingredientes" : l.fichaStatus;
  const rodar = async (fn: () => void | Promise<void>) => {
    setOcupado(true);
    try {
      await fn();
    } finally {
      setOcupado(false);
    }
  };
  return (
    <tr className={l.feito ? "off" : ""}>
      <td className="wrapc">
        <b>{l.nome ?? "Ficha removida"}</b>
      </td>
      <td>{l.praca}</td>
      <td className="r">
        {l.feito ? (
          <b>{fmtN(l.qtd)}</b>
        ) : (
          <input
            className="inline-num"
            style={{ textAlign: "right" }}
            inputMode="decimal"
            value={q}
            aria-label={`Quantidade de ${l.nome ?? "item"}`}
            onChange={(e) => setQ(e.target.value)}
            onBlur={() => {
              const n = numIn(q);
              if (!(n > 0)) {
                showToast("Informe uma quantidade maior que zero.", "error");
                setQ(dec(l.qtd));
              } else if (Math.abs(n - l.qtd) > 1e-9) onQtd(n);
            }}
          />
        )}{" "}
        {l.unidade}
      </td>
      <td className="r">{fmtN(Math.round(l.receitas * 100) / 100)}</td>
      <td>
        <Pill tom={tom}>{rot}</Pill>
      </td>
      <td>
        {l.feito ? (
          <>
            <Pill tom="good">Feito {fdate(l.feitoEm)}</Pill>
            {l.lote && (
              <button type="button" className="btn sm ghost" onClick={onLote}>
                Lote {l.lote.codigo}
              </button>
            )}
          </>
        ) : (
          <Pill>A produzir</Pill>
        )}
      </td>
      <td style={{ textAlign: "right" }}>
        {l.feito ? (
          <button type="button" className="btn sm ghost" disabled={ocupado} onClick={() => rodar(onDesfazer)}>
            Desfazer
          </button>
        ) : (
          <>
            <button type="button" className="btn sm primary" disabled={ocupado || !l.nome} onClick={() => rodar(onConcluir)}>
              Concluir
            </button>{" "}
            <button type="button" className="btn sm ghost" aria-label="Tirar do plano" disabled={ocupado} onClick={() => rodar(onRemover)}>
              ✕
            </button>
          </>
        )}
      </td>
    </tr>
  );
}

function AbaInsumosPlano({ d }: { d: PlanoDetalheResponseDTO }) {
  const its = d.insumos.filter((x) => x.qtd > 0);
  if (!its.length)
    return (
      <section className="panel">
        <Vazio>Informe as quantidades na lista de produção.</Vazio>
      </section>
    );
  return (
    <>
      {its.map((x) => (
        <section key={x.k} className="panel czcard">
          <div className="panel-h">
            <div style={{ minWidth: 0 }}>
              <span className="m" style={{ fontSize: 12, color: "var(--muted)" }}>
                {x.categoriaLabel} · {x.praca}
              </span>
              <div style={{ fontWeight: 700 }}>{x.nome}</div>
              <span className="m" style={{ fontSize: 13, color: "var(--muted)" }}>
                A produzir <b>{qtdU(x.qtd, x.unidade)}</b> · ficha rende {x.rendimentoLabel}
                {x.feito && (
                  <>
                    {" "}
                    · <b>feito</b>
                  </>
                )}
              </span>
            </div>
          </div>
          {x.ins.length ? (
            <div className="tablewrap">
              <table>
                <thead>
                  <tr>
                    <th>Insumo</th>
                    <th className="r">Por receita</th>
                    <th className="r">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {x.ins.map((i, ix) => (
                    <tr key={ix}>
                      <td className="wrapc">
                        {i.n}
                        {i.via && (
                          <>
                            <br />
                            <span className="note">via {i.via}</span>
                          </>
                        )}
                      </td>
                      <td className="r">{qtdU(i.qb, i.u)}</td>
                      <td className="r">
                        <b>{qtdU(i.qt, i.u)}</b>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <Vazio>Esta ficha ainda não tem ingredientes. Complete em Fichas.</Vazio>
          )}
          {x.modoPreparo && (
            <details className="czdet">
              <summary>Modo de preparo</summary>
              <p style={{ whiteSpace: "pre-wrap", margin: "8px 0 0" }}>{x.modoPreparo}</p>
            </details>
          )}
        </section>
      ))}
    </>
  );
}

function AbaComprasPlano({ d }: { d: PlanoDetalheResponseDTO }) {
  const c = d.compras;
  return (
    <>
      <div className="kpis">
        <Kpi k="Insumos necessários" v={String(c.kpis.necessarios)} h="para os itens a produzir" />
        <Kpi k="A comprar" v={String(c.kpis.aComprar)} h={c.kpis.aComprar ? "faltam no estoque" : "estoque cobre tudo"} />
        <Kpi k="Custo estimado" v={brl(c.kpis.custo)} h={c.kpis.semPreco ? `${c.kpis.semPreco} sem preço` : "pelo último preço"} />
        <Kpi k="Itens concluídos" v={String(c.kpis.concluidos)} h="já saíram da conta" />
      </div>
      <section className="panel">
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                <th>Insumo</th>
                <th className="r">Precisa</th>
                <th className="r">Em estoque</th>
                <th className="r">Comprar</th>
                <th className="r">Custo estimado</th>
                <th>Usado em</th>
              </tr>
            </thead>
            <tbody>
              {!c.itens.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Nada a comprar: todos os itens já foram concluídos ou as fichas não têm ingredientes.
                  </td>
                </tr>
              )}
              {c.itens.map((x) => (
                <tr key={x.insumoId}>
                  <td className="wrapc">
                    <b>{x.nome}</b>
                  </td>
                  <td className="r">{qtdU(x.precisa, x.unidade)}</td>
                  <td className="r">{qtdU(x.estoque, x.unidade)}</td>
                  <td className="r">{x.falta > 0.0005 ? <b>{qtdU(x.falta, x.unidade)}</b> : <span className="pos">ok</span>}</td>
                  <td className="r">{x.falta > 0.0005 ? x.semPreco ? <span className="warn-t">sem preço</span> : brl(x.custo) : "—"}</td>
                  <td className="wrapc note">{x.usos.join(", ")}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <p className="note">Considera só os itens ainda não concluídos. &quot;Em estoque&quot; é o saldo atual do insumo, que já desconta os lotes concluídos.</p>
    </>
  );
}

/* ------------------------------- Formulários ------------------------------- */

function PlanoForm({ plano, unidade, onCriado, onExcluido }: { plano?: PlanoProducaoResponseDTO; unidade: UnidadePlano; onCriado?: (id: number) => void; onExcluido?: () => void }) {
  const t = hoje();
  const { fechar } = useSheet();
  const [v, setV] = useState({
    titulo: plano?.titulo ?? "",
    unidade: plano?.unidade ?? unidade,
    status: plano?.status ?? ("planejado" as StatusPlano),
    dataInicio: plano?.dataInicio ?? t,
    dataFim: plano?.dataFim ?? (unidade === "chocolates" ? t : addDays(t, 6)),
    obs: plano?.obs ?? "",
  });
  const dto = () => ({ ...v, titulo: v.titulo.trim(), dataFim: v.dataFim || v.dataInicio });
  return (
    <FormSheet
      validar={() => (!v.titulo.trim() ? "Dê um nome ao plano." : !v.dataInicio ? "Informe a data de início." : v.dataFim && v.dataFim < v.dataInicio ? "A data final não pode ser antes do início." : null)}
      onSalvar={() => (plano ? atualizarPlano(plano.id, dto()) : criarPlano(dto()))}
      okMsg={plano ? "Plano salvo" : "Plano criado"}
      depois={
        plano
          ? undefined
          : (r) => {
              fechar();
              onCriado?.((r as PlanoProducaoResponseDTO).id);
            }
      }
      onExcluir={plano ? () => excluirPlano(plano.id) : null}
      excluirLabel="Excluir plano"
      excluirConfirma="Excluir este plano? Os lotes já registrados continuam em Pré-preparos."
      depoisExcluir={onExcluido}
    >
      <Fld label="Nome do plano" obrig full>
        <input
          value={v.titulo}
          onChange={(e) => setV({ ...v, titulo: e.target.value })}
          placeholder={v.unidade === "chocolates" ? "Ex.: Chocolates · Natal (bombons e tabletes)" : `Ex.: Restaurante · semana ${fdate(t).slice(0, 5)} a ${fdate(addDays(t, 6)).slice(0, 5)}`}
          autoFocus
        />
      </Fld>
      <Fld label="Unidade">
        <Sel value={v.unidade} onChange={(x) => setV({ ...v, unidade: x as UnidadePlano })} opcoes={UNIDADES_PLANO} />
      </Fld>
      <Fld label="Situação">
        <Sel value={v.status} onChange={(x) => setV({ ...v, status: x as StatusPlano })} opcoes={STATUS_PLANO} />
      </Fld>
      <Fld label="De" obrig>
        <input type="date" value={v.dataInicio} onChange={(e) => setV({ ...v, dataInicio: e.target.value })} />
      </Fld>
      <Fld label="Até">
        <input type="date" value={v.dataFim} onChange={(e) => setV({ ...v, dataFim: e.target.value })} />
      </Fld>
      <Fld label="Observações" full>
        <textarea value={v.obs} onChange={(e) => setV({ ...v, obs: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}

const norm = (t: string) =>
  t
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase();

/** Marcar fichas e quantidades (vem preenchido com o rendimento de uma receita). */
function AdicionarItensSheet({ plano }: { plano: PlanoProducaoResponseDTO }) {
  const { dados: preps, erro } = useDados(() => listarPreparacoes());
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [sel, setSel] = useState<Record<number, string>>({});
  const [busca, setBusca] = useState("");
  const [cat, setCat] = useState("");
  const [ocupado, setOcupado] = useState(false);
  if (!preps) return <Carregando erro={erro} />;
  if (!preps.length) return <Vazio titulo="Nenhuma ficha técnica">Cadastre as fichas técnicas primeiro, na aba Fichas.</Vazio>;
  const ja = new Set(plano.itens.filter((x) => !x.feito).map((x) => x.preparacaoId));
  const q = norm(busca.trim());
  const vis = preps.filter((p) => (!cat || p.categoria === cat) && (!q || norm(p.nome).includes(q)));
  const marcados = Object.keys(sel).length;
  const unid = (u: string, n: number) => (u === "porções" ? (n === 1 ? "porção" : "porções") : u === "un" ? "un" : u);

  async function salvar() {
    const itens = Object.entries(sel).map(([id, v]) => ({ preparacaoId: Number(id), qtd: Math.round(numIn(v) * 1000) / 1000 }));
    if (itens.some((x) => !(x.qtd > 0))) return showToast("Informe a quantidade de todos os itens marcados.", "error");
    setOcupado(true);
    try {
      await adicionarItensPlano(plano.id, { itens });
      showToast(`${itens.length} ite${itens.length === 1 ? "m adicionado" : "ns adicionados"}`);
      recarregar();
      fechar();
    } catch (e) {
      showToast(msgErro(e), "error");
      setOcupado(false);
    }
  }

  return (
    <>
      <p className="note">Marque o que vai ser produzido e ajuste a quantidade, na unidade da ficha. Vem preenchido com o rendimento de uma receita.</p>
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar ficha…" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar ficha" />
      </div>
      <Chips opcoes={[["", "Todas"], ...CATEGORIAS_PREPARACAO.filter(([k]) => preps.some((p) => p.categoria === k))]} valor={cat} onChange={setCat} rotulo="Categoria" />
      <div className="box">
        {!vis.length && <Vazio titulo="Nenhuma ficha encontrada">Mude a busca ou a categoria.</Vazio>}
        {vis.map((p) => {
          const tem = ja.has(p.id);
          const marcado = p.id in sel;
          return (
            <div key={p.id} className={`row static${tem ? " off" : ""}`}>
              <input
                type="checkbox"
                className="czck"
                aria-label={p.nome}
                checked={tem || marcado}
                disabled={tem}
                onChange={(e) =>
                  setSel((s) => {
                    const n = { ...s };
                    if (e.target.checked) n[p.id] = dec(p.rendimento);
                    else delete n[p.id];
                    return n;
                  })
                }
              />
              <span className="grow">
                <span className="t wrap">{p.nome}</span>
                <span className="m">
                  {tem ? "Já está no plano" : `Receita rende ${fmtN(p.rendimento)} ${unid(p.unidadeRendimento, p.rendimento)}${p.itens.length ? "" : " · sem ingredientes"}`}
                </span>
              </span>
              {!tem && (
                <span style={{ display: "flex", alignItems: "center", gap: 6, flex: "none" }}>
                  <input className="inline-num" style={{ width: 76, textAlign: "right" }} inputMode="decimal" disabled={!marcado} value={sel[p.id] ?? dec(p.rendimento)} aria-label={`Quantidade de ${p.nome}`} onChange={(e) => setSel((s) => ({ ...s, [p.id]: e.target.value }))} />
                  <span className="m" style={{ width: 52 }}>
                    {unid(p.unidadeRendimento, 2)}
                  </span>
                </span>
              )}
            </div>
          );
        })}
      </div>
      <div className="actions">
        <span className="note left">
          <b>{marcados}</b> marcado{marcados === 1 ? "" : "s"}
        </span>
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="button" className="btn primary" disabled={!marcados || ocupado} onClick={salvar}>
          {marcados ? `Adicionar ${marcados} ite${marcados === 1 ? "m" : "ns"}` : "Adicionar"}
        </button>
      </div>
    </>
  );
}

/** Pratos da carta previstos no período → fichas a produzir. */
function CartaSheet({ plano }: { plano: PlanoProducaoResponseDTO }) {
  const { dados, erro } = useDados(() => telaPratosVenda());
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [prev, setPrev] = useState<Record<string, number>>(plano.previsao ?? {});
  const [ocupado, setOcupado] = useState(false);
  if (!dados) return <Carregando erro={erro} />;
  const pvs = dados.pratos.filter((x) => x.ativo && x.componentes.length);
  if (!pvs.length) return <Vazio titulo="Nenhum prato na carta">Cadastre os pratos do restaurante em Fichas › Pratos do restaurante.</Vazio>;
  const total = Object.values(prev).reduce((a, b) => a + b, 0);

  async function salvar() {
    if (!total) return showToast("Informe ao menos um prato previsto.", "error");
    setOcupado(true);
    try {
      const r = await calcularPelaCarta(plano.id, { previsao: prev });
      showToast(`${r.fichas} ficha${r.fichas === 1 ? "" : "s"} no plano`);
      recarregar();
      fechar();
    } catch (e) {
      showToast(msgErro(e), "error");
      setOcupado(false);
    }
  }

  return (
    <>
      <p className="note">Informe quantos pratos de cada um você prevê vender no período. O sistema soma as fichas que cada prato leva e coloca no plano o que falta produzir.</p>
      <div className="box">
        {SECOES_CARTA.map(([s, rot]) => {
          const ps = pvs.filter((x) => x.secao === s).sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));
          if (!ps.length) return null;
          return (
            <div key={s}>
              <div className="monthhead">{rot}</div>
              {ps.map((x) => (
                <div key={x.id} className="row static">
                  <span className="grow">
                    <span className="t">{x.nome}</span>
                    <span className="m">{x.componentesNomes.join(" · ")}</span>
                  </span>
                  <input
                    className="inline-num"
                    style={{ width: 76, textAlign: "right", flex: "none" }}
                    inputMode="numeric"
                    placeholder="0"
                    aria-label={`Pratos previstos de ${x.nome}`}
                    value={prev[x.id] || ""}
                    onChange={(e) =>
                      setPrev((p) => {
                        const n = { ...p };
                        const v = Math.max(0, Math.round(numIn(e.target.value)));
                        if (v) n[x.id] = v;
                        else delete n[x.id];
                        return n;
                      })
                    }
                  />
                </div>
              ))}
            </div>
          );
        })}
      </div>
      <div className="actions">
        <span className="note left">{total ? `${total} pratos previstos` : "—"}</span>
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="button" className="btn primary" disabled={ocupado} onClick={salvar}>
          Colocar no plano
        </button>
      </div>
    </>
  );
}
