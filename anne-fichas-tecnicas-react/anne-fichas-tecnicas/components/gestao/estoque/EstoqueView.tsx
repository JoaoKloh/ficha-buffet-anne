"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { Header } from "@/components/Header";
import { Carregando, Chips, Datebox, Kpi, Pill, Vazio } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { LOCAIS_ESTOQUE, TIPOS_ITEM, reutilizavel, rotulo } from "@/lib/models/dominio";
import { listarInsumos } from "@/lib/api/gestao/fichas";
import { listarMovimentos, listarNfs, obterNf, telaLoucas, telaPreparos, telaSaldo } from "@/lib/api/gestao/estoque";
import { brl, fdate, fmtN, qtdU } from "@/lib/utils/numero";
import { InsumoForm } from "../fichas/InsumoForm";
import { InventarioSheet, ItemSheet, MovForm, MovRow, MovSheet } from "./EstoqueForms";
import { LoteForm, LoteRow, LoteSheet } from "./Lotes";
import { NfSheet, useImportarNf } from "./NfSheet";
import { AbaCompras, AbaConferencia, AbaPerdas, AbaValidades } from "./Integrado";

const ABAS = [
  ["saldo", "Saldo"],
  ["compras", "Compras"],
  ["validades", "Validades"],
  ["preparos", "Pré-preparos"],
  ["itens", "Itens"],
  ["movimentos", "Movimentos"],
  ["notas", "Notas fiscais"],
  ["perdas", "Perdas"],
  ["loucas", "Louças"],
  ["conferencia", "Conferência"],
] as const;
const ehAba = (s: string | null | undefined): s is Aba => ABAS.some((a) => a[0] === s);
type Aba = (typeof ABAS)[number][0];

/** `?aba=compras|validades|…` abre direto numa aba (atalhos do Painel). */
export function EstoqueView() {
  const sp = useSearchParams();
  const abaUrl = sp.get("aba");
  return <Tela key={abaUrl ?? ""} abaInicial={ehAba(abaUrl) ? abaUrl : "saldo"} />;
}

function Tela({ abaInicial }: { abaInicial: Aba }) {
  const [aba, setAba] = useState<Aba>(abaInicial);
  const { abrir } = useSheet();
  const nf = useImportarNf();
  const { dados: saldo } = useDados(telaSaldo);
  const acoes =
    aba === "preparos" ? (
      <button type="button" className="btn primary" onClick={() => abrir("Novo lote de pré-preparo", <LoteForm />)}>
        Novo lote
      </button>
    ) : aba === "itens" ? (
      <button type="button" className="btn primary" onClick={() => abrir("Novo item", <InsumoForm />)}>
        Novo item
      </button>
    ) : aba === "notas" ? (
      <>
        <button type="button" className="btn" onClick={nf.escolherFoto}>
          Ler nota pela foto
        </button>
        <button type="button" className="btn" onClick={nf.escolherXml}>
          Importar XML da NF-e
        </button>
        <button type="button" className="btn primary" onClick={() => abrir("Nova nota fiscal", <NfSheet />, { larga: true })}>
          Nova nota fiscal
        </button>
      </>
    ) : aba === "loucas" ? (
      <button type="button" className="btn primary" onClick={() => abrir("Nova louça ou utensílio", <InsumoForm tipoInicial="louca" />)}>
        Nova louça ou utensílio
      </button>
    ) : aba === "compras" || aba === "validades" || aba === "perdas" || aba === "conferencia" ? (
      <button type="button" className="btn primary" onClick={() => abrir("Lançar movimento", <MovForm />)}>
        Lançar movimento
      </button>
    ) : (
      <>
        <button type="button" className="btn" onClick={() => abrir(saldo?.semMovimentos ? "Inventário inicial" : "Inventário", <InventarioSheet inicial={!!saldo?.semMovimentos} />, { larga: true })}>
          Fazer inventário
        </button>
        <button type="button" className="btn primary" onClick={() => abrir("Lançar movimento", <MovForm />)}>
          Lançar movimento
        </button>
      </>
    );

  return (
    <div className="app">
      {nf.inputs}
      <Header sub="Saldo calculado pelos movimentos" actions={acoes} />
      <Chips opcoes={ABAS} valor={aba} onChange={setAba} rotulo="Seções do estoque" />
      {saldo?.semMovimentos && (
        <div className="banner">
          O estoque ainda não tem saldo. Comece pelo <b>inventário inicial</b>: conte o que há na cozinha e no depósito e lance as quantidades em Fazer inventário.
        </div>
      )}
      {aba === "saldo" && <AbaSaldo />}
      {aba === "preparos" && <AbaPreparos />}
      {aba === "itens" && <AbaItens />}
      {aba === "movimentos" && <AbaMovimentos />}
      {aba === "notas" && <AbaNotas />}
      {aba === "loucas" && <AbaLoucas />}
      {aba === "compras" && <AbaCompras />}
      {aba === "validades" && <AbaValidades />}
      {aba === "perdas" && <AbaPerdas />}
      {aba === "conferencia" && <AbaConferencia />}
    </div>
  );
}

function AbaSaldo() {
  const { abrir } = useSheet();
  const { dados, erro } = useDados(telaSaldo);
  const [filtro, setFiltro] = useState<"todos" | "atencao" | "comsaldo">("todos");
  const [local, setLocal] = useState("");
  const [busca, setBusca] = useState("");
  if (!dados) return <Carregando erro={erro} />;
  const k = dados.kpis;
  const q = busca.trim().toLowerCase();
  const cons = dados.itens.filter((x) => !reutilizavel(x.insumo.tipo));
  const rows = cons.filter(
    (x) => (!q || x.insumo.nome.toLowerCase().includes(q)) && (!local || x.insumo.local === local) && (filtro === "todos" || (filtro === "comsaldo" ? x.fisico !== 0 : x.situacao[0] === "bad" || x.situacao[0] === "warn"))
  );
  return (
    <>
      <div className="kpis">
        <Kpi k="Valor em estoque" v={brl(k.valor)} h={k.semPreco ? `${k.semPreco} ite${k.semPreco > 1 ? "ns" : "m"} com saldo sem preço` : "consumíveis a preço atual"} />
        <Kpi k="Itens com saldo" v={String(k.comSaldo)} h={`de ${k.consumiveis} consumíveis`} />
        <Kpi k="Comprar ou abaixo do mínimo" v={String(k.abaixo)} negativo={k.abaixo > 0} h="disponível contra reservas e mínimo" />
        <Kpi k="Saldo negativo" v={String(k.negativos)} negativo={k.negativos > 0} h={k.negativos ? "confira a contagem" : "nenhum"} />
      </div>
      <div className="toolbar">
        <Chips opcoes={[["todos", "Todos"], ["atencao", "Precisa de atenção"], ["comsaldo", "Com saldo"]] as const} valor={filtro} onChange={setFiltro} rotulo="Filtro do saldo" />
        <select className="sel-inline" aria-label="Local" value={local} onChange={(e) => setLocal(e.target.value)}>
          <option value="">Todos os locais</option>
          {LOCAIS_ESTOQUE.map((l) => (
            <option key={l}>{l}</option>
          ))}
        </select>
        <input className="search" type="search" placeholder="Buscar item" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar item" />
      </div>
      <p className="note">Disponível = físico − reservado. A reserva nasce quando o evento é contratado (pratos Sim) e termina quando o prato é marcado como produzido no checklist, que dá a baixa. Louças e utensílios ficam na aba Louças.</p>
      <section className="panel">
        <div className="panel-h">
          <h3>Consumíveis · ingredientes e embalagens</h3>
          <span className="note">
            {rows.length} ite{rows.length === 1 ? "m" : "ns"}
          </span>
        </div>
        {!rows.length ? (
          <Vazio>{cons.length ? "Nenhum item neste filtro." : "Nenhum item cadastrado. Cadastre em Itens."}</Vazio>
        ) : (
          <div className="tablewrap">
            <table>
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Local</th>
                  <th className="r">Físico</th>
                  <th className="r">Reservado</th>
                  <th className="r">Disponível</th>
                  <th className="r">Mínimo</th>
                  <th className="r">Valor</th>
                  <th>Situação</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((x) => {
                  const i = x.insumo;
                  return (
                    <tr key={i.id} className="clicavel" onClick={() => abrir(i.nome, <ItemSheet id={i.id} />)}>
                      <td className="wrapc">
                        <b>{i.nome}</b>
                        <br />
                        <span className="note">{rotulo(TIPOS_ITEM, i.tipo, "Item")}</span>
                      </td>
                      <td>{i.local}</td>
                      <td className={`r${x.fisico < 0 ? " neg" : ""}`}>
                        <b>
                          {x.fisico < 0 ? "−" : ""}
                          {qtdU(x.fisico, i.unidade)}
                        </b>
                      </td>
                      <td className="r">{x.reservado ? qtdU(x.reservado, i.unidade) : "—"}</td>
                      <td className={`r${x.disponivel < 0 ? " neg" : ""}`}>
                        <b>
                          {x.disponivel < 0 ? "−" : ""}
                          {qtdU(x.disponivel, i.unidade)}
                        </b>
                      </td>
                      <td className="r">{i.estoqueMinimo > 0 ? qtdU(i.estoqueMinimo, i.unidade) : "—"}</td>
                      <td className="r">{x.valor ? brl(x.valor) : "—"}</td>
                      <td>
                        <Pill tom={x.situacao[0]}>{x.situacao[1]}</Pill>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

function AbaItens() {
  const { abrir } = useSheet();
  const [busca, setBusca] = useState("");
  const { dados, erro } = useDados(() => listarInsumos(busca), [busca]);
  return (
    <>
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar item" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar item" />
      </div>
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.length ? (
            <Vazio titulo={busca ? "Nenhum item encontrado" : "Nenhum item cadastrado"}>Ingredientes, embalagens, louças e utensílios ficam num cadastro só. As fichas e o estoque usam este cadastro.</Vazio>
          ) : (
            TIPOS_ITEM.map(([tp, l]) => {
              const its = dados.filter((i) => i.tipo === tp);
              if (!its.length) return null;
              return (
                <div key={tp}>
                  <div className="monthhead">
                    {l} · {reutilizavel(tp) ? "reutilizável" : "consumível"}
                  </div>
                  {its.map((i) => (
                    <button key={i.id} type="button" className="row" onClick={() => abrir("Editar item", <InsumoForm insumo={i} />)}>
                      <span className="grow">
                        <span className="t">{i.nome}</span>
                        <span className="m">{[i.unidade + (i.unidadeCompra ? ` · compra ${i.unidadeCompra}` : ""), i.local, i.emFichas ? `em ${i.emFichas} ficha${i.emFichas > 1 ? "s" : ""}` : null, i.fornecedor].filter(Boolean).join(" · ")}</span>
                      </span>
                      {i.custo > 0 ? (
                        <span className="amt">
                          {brl(i.custo)}
                          <span className="m" style={{ display: "inline" }}>
                            {" "}
                            /{i.unidade}
                          </span>
                        </span>
                      ) : (
                        <Pill tom="warn">sem preço</Pill>
                      )}
                    </button>
                  ))}
                </div>
              );
            })
          )}
        </div>
      </section>
    </>
  );
}

const FILTROS_MOV = [
  ["todos", "Todos"],
  ["entradas", "Entradas"],
  ["saidas", "Saídas"],
  ["contagens", "Inventário e ajustes"],
] as const;

function AbaMovimentos() {
  const { abrir } = useSheet();
  const [filtro, setFiltro] = useState<(typeof FILTROS_MOV)[number][0]>("todos");
  const { dados, erro } = useDados(() => listarMovimentos(filtro), [filtro]);
  return (
    <>
      <Chips opcoes={FILTROS_MOV} valor={filtro} onChange={setFiltro} rotulo="Filtro de movimentos" />
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.movimentos.length ? (
            <Vazio titulo="Nenhum movimento">Inventário, entradas, perdas, ajustes, devoluções e sobras aparecem aqui, do mais recente para o mais antigo.</Vazio>
          ) : (
            dados.movimentos.map((m, i) => <MovRow key={m.virtual ? `v${i}` : m.id} m={m} onClick={m.virtual ? undefined : () => abrir("Movimento", <MovSheet m={m} />)} />)
          )}
        </div>
      </section>
      {dados && dados.total > 300 && <p className="note">Mostrando os 300 mais recentes.</p>}
    </>
  );
}

function AbaNotas() {
  const { abrir } = useSheet();
  const { dados, erro } = useDados(listarNfs);
  return (
    <>
      <p className="note">
        Importe o XML da NF-e (o fornecedor manda por e-mail), leia a nota por foto ou lance à mão. Em cada nota você define o <b>destino</b> (estoque da fábrica ou um evento) e o <b>pagamento</b> com as parcelas do boleto. Ao dar entrada, o estoque entra, o preço dos itens se
        atualiza e cada parcela vira uma conta a pagar no vencimento.
      </p>
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.length ? (
            <Vazio titulo="Nenhuma nota fiscal">Ao receber mercadoria, importe o XML ou lance a nota: o estoque entra, o preço dos itens se atualiza e as parcelas vão para contas a pagar.</Vazio>
          ) : (
            dados.map((n) => {
              const pag = n.formaPag ? `${n.formaPag === "prazo" ? "A prazo" : n.formaPag.charAt(0).toUpperCase() + n.formaPag.slice(1)}${n.parcelas.length > 1 ? ` ${n.parcelas.length}x` : ""}` : null;
              const meta = [`${n.itens.length} ite${n.itens.length === 1 ? "m" : "ns"}`, n.eventoNome ? `→ ${n.eventoCodigo} · ${n.eventoNome}` : "→ Estoque da fábrica", pag, n.proximoVencimento ? `próx. venc. ${fdate(n.proximoVencimento)}` : n.quitada ? "quitada" : null].filter(Boolean).join(" · ");
              return (
                <button key={n.id} type="button" className="row" onClick={async () => abrir(`NF ${n.numero || "s/n"}`, <NfSheet nf={await obterNf(n.id)} />, { larga: true })}>
                  <Datebox iso={n.emissao} />
                  <span className="grow">
                    <span className="t">
                      NF {n.numero || "s/n"} · {n.fornecedor || "Fornecedor"}
                    </span>
                    <span className="m">{meta}</span>
                  </span>
                  <Pill tom={n.status === "lancada" ? "good" : "warn"}>{n.status === "lancada" ? "Lançada" : "Rascunho"}</Pill>
                  <span className="amt">{brl(n.total)}</span>
                </button>
              );
            })
          )}
        </div>
      </section>
      <div className="summary">
        <span>
          <b>Sobre a importação:</b> o XML é exato; a foto precisa ser conferida linha a linha (a leitura por foto ainda não está disponível neste sistema). Itens, quantidades, valores com frete/IPI/desconto rateados, fornecedor e as duplicatas do boleto são lidos do arquivo.
          A mesma nota não entra duas vezes. Buscar a nota direto na SEFAZ exige certificado digital A1 e fica para depois.
        </span>
      </div>
    </>
  );
}

function AbaLoucas() {
  const { abrir } = useSheet();
  const [data, setData] = useState<string | null>(null);
  const { dados, erro } = useDados(() => telaLoucas(data), [data]);
  if (!dados) return <Carregando erro={erro} />;
  const d5 = fdate(dados.data).slice(0, 5);
  return (
    <>
      <p className="note">Louças e utensílios não são consumidos: são reservados por data de evento, saem e voltam pelo checklist de envio. Avaria ou perda reduz o total, sempre com motivo.</p>
      <div className="toolbar">
        <label className="fator">
          <span className="m">Disponibilidade em</span>
          <input type="date" className="sel-inline" value={dados.data} onChange={(e) => setData(e.target.value || null)} />
        </label>
        <span className="note">{dados.eventosNaData.length ? dados.eventosNaData.map((e) => `${e.codigo} · ${e.nome}`).join(", ") : "Nenhum evento nesta data"}</span>
      </div>
      <section className="panel">
        {!dados.itens.length ? (
          <Vazio titulo="Nenhuma louça ou utensílio cadastrado">Cadastre em Novo item com tipo Louça ou Utensílio, faça a contagem pelo inventário e inclua nas fichas.</Vazio>
        ) : (
          <div className="tablewrap">
            <table>
              <thead>
                <tr>
                  <th>Item</th>
                  <th className="r">Total</th>
                  <th className="r">Reservado {d5}</th>
                  <th className="r">Fora (em eventos)</th>
                  <th className="r">Disponível {d5}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {dados.itens.map((x) => (
                  <tr key={x.insumo.id}>
                    <td className="wrapc">
                      <b>{x.insumo.nome}</b>
                      <br />
                      <span className="note">
                        {rotulo(TIPOS_ITEM, x.insumo.tipo)}
                        {x.reservas.length ? ` · ${x.reservas.map((r) => `${r.codigo}: ${fmtN(r.qtd)}`).join(", ")}` : ""}
                      </span>
                    </td>
                    <td className="r">{fmtN(x.total)}</td>
                    <td className="r">{x.reservado ? fmtN(x.reservado) : "—"}</td>
                    <td className="r">{x.fora ? fmtN(x.fora) : "—"}</td>
                    <td className={`r${x.disponivel < 0 ? " neg" : ""}`}>
                      <b>{fmtN(x.disponivel)}</b>
                    </td>
                    <td>
                      <button type="button" className="btn sm" onClick={() => abrir("Lançar movimento", <MovForm tipo="perda" insumoId={x.insumo.id} />)}>
                        Avaria
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

const FILTROS_LOTE = [
  ["ativos", "Com saldo"],
  ["atencao", "Precisa de atenção"],
  ["todos", "Todos"],
] as const;

function AbaPreparos() {
  const { abrir } = useSheet();
  const [filtro, setFiltro] = useState<(typeof FILTROS_LOTE)[number][0]>("ativos");
  const { dados, erro } = useDados(() => telaPreparos(filtro), [filtro]);
  if (!dados) return <Carregando erro={erro} />;
  const k = dados.kpis;
  const unidRend = (u: string, n: number) => (u === "porções" ? (n === 1 ? "porção" : "porções") : u === "un" ? "un" : u);
  return (
    <>
      <div className="kpis">
        <Kpi k="Lotes com saldo" v={String(k.comSaldo)} h={k.comSaldo ? `de ${k.preparacoes} preparações` : "nada pronto"} />
        <Kpi k="Vencem hoje ou amanhã" v={String(k.vencemHojeAmanha)} negativo={k.vencemHojeAmanha > 0} h={k.vencemHojeAmanha ? "use primeiro" : "nenhum"} />
        <Kpi k="Vencidos com saldo" v={String(k.vencidos)} negativo={k.vencidos > 0} h={k.vencidos ? "descarte e registre a perda" : "nenhum"} />
        <Kpi k="Valor em pré-preparos" v={brl(k.valor)} h="a custo de ficha" />
      </div>
      <section className="panel">
        <div className="panel-h">
          <h3>Pronto agora</h3>
          <span className="note">use primeiro o lote indicado</span>
        </div>
        {!dados.prontoAgora.length ? (
          <Vazio titulo="Nenhum pré-preparo pronto">Registre cada produção em Novo lote: molhos, massas, caldos, recheios. O saldo cai conforme a cozinha usa ou descarta.</Vazio>
        ) : (
          <div className="tablewrap">
            <table>
              <thead>
                <tr>
                  <th>Preparação</th>
                  <th className="r">Pronto</th>
                  <th>Usar primeiro</th>
                  <th>Validade</th>
                </tr>
              </thead>
              <tbody>
                {dados.prontoAgora.map((p) => (
                  <tr key={p.preparacaoId} className="clicavel" onClick={() => abrir("Lote", <LoteSheet id={p.usarPrimeiro.id} />)}>
                    <td className="wrapc">
                      <b>{p.nome}</b>
                      {p.lotes > 1 && (
                        <>
                          <br />
                          <span className="note">{p.lotes} lotes</span>
                        </>
                      )}
                    </td>
                    <td className="r">
                      <b>
                        {fmtN(p.total)} {unidRend(p.unidade, p.total)}
                      </b>
                    </td>
                    <td className="num">{p.usarPrimeiro.codigo || "—"}</td>
                    <td>
                      <Pill tom={p.usarPrimeiro.situacao[0]}>{p.usarPrimeiro.validade ? fdate(p.usarPrimeiro.validade) : "—"}</Pill>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <Chips opcoes={FILTROS_LOTE} valor={filtro} onChange={setFiltro} rotulo="Filtro de lotes" />
      <section className="panel">
        <div className="panel-h">
          <h3>Lotes</h3>
          <span className="note">ordenados pela validade</span>
        </div>
        <div className="panel-b">{dados.lotes.length ? dados.lotes.map((l) => <LoteRow key={l.id} l={l} onClick={() => abrir("Lote", <LoteSheet id={l.id} />)} />) : <Vazio>Nenhum lote neste filtro.</Vazio>}</div>
      </section>
      <p className="note">Lote para o restaurante ou para o estoque geral dá baixa nos insumos quando é registrado. Lote feito para um evento não dá baixa aqui, porque o checklist de envio do evento já dá.</p>
    </>
  );
}
