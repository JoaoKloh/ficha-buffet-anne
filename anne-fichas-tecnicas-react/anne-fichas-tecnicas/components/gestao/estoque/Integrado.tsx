"use client";

import { useState } from "react";
import { Carregando, Chips, CopiarTexto, Datebox, Kpi, Pill, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { HORIZONTES_COMPRA, type HorizonteCompra } from "@/lib/models/dominio";
import { GRUPOS_PERDA, normalizarCompras, normalizarConferencia, normalizarPerdas, normalizarValidades, type SugestaoCompraDTO, type ValidadeItemDTO } from "@/lib/models/estoque";
import { marcarPedido, telaCompras, telaConferencia, telaPerdas, telaValidades } from "@/lib/api/gestao/estoque";
import { brl, fdate, fmtN, pct, qtdU, ymLabel } from "@/lib/utils/numero";
import { addMonths, hoje } from "@/lib/utils/datas";
import { ItemSheet, MovForm } from "./EstoqueForms";
import { LoteSheet } from "./Lotes";
import { NfSheet } from "./NfSheet";
import { obterNf } from "@/lib/api/gestao/estoque";

/**
 * Estoque integrado (protótipo: estCompras, estValidades, estPerdas, estConferencia).
 * Toda conta vem pronta do backend; aqui só filtros, agrupamento visual e texto do pedido.
 * Os blocos (KPIs, resumos e painéis) aparecem sempre: sem resposta ainda, os KPIs mostram "—"
 * e o painel mostra o carregamento ou o erro; com resposta vazia, o painel diz que não há nada.
 */

const HZ = HORIZONTES_COMPRA.map(([d, l]) => [String(d), l] as const);
const comprarTxt = (x: SugestaoCompraDTO) => (x.embalagens ? `${x.embalagens}× ${x.unidadeCompra || "emb."}` : qtdU(x.comprar, x.unidade));

export function AbaCompras() {
  const { abrir, recarregar } = useSheet();
  const { showToast } = useToast();
  const [hz, setHz] = useState<HorizonteCompra>(15);
  const { dados, erro } = useDados(() => telaCompras(hz), [hz]);
  const ok = dados != null;
  const t = dados ?? normalizarCompras(null, hz);

  async function pedido(insumoId: number, feito: boolean) {
    try {
      await marcarPedido({ insumoId, feito });
      showToast(feito ? "Pedido marcado" : "Pedido desmarcado");
      recarregar();
    } catch (e) {
      showToast(msgErro(e), "error");
    }
  }

  return (
    <>
      <p className="note">
        Comprar = o que os eventos dos próximos {hz} dias vão usar (pratos confirmados ainda não produzidos) + estoque mínimo − saldo físico. Arredonda para a embalagem de compra
        quando o item tem fator cadastrado. Marque &quot;pedido feito&quot; para tirar da lista até a mercadoria entrar.
      </p>
      <Chips opcoes={HZ} valor={String(hz)} onChange={(x) => setHz(Number(x) as HorizonteCompra)} rotulo="Horizonte das compras" />
      <div className="kpis">
        <Kpi k="Itens a comprar" v={ok ? String(t.kpis.itens) : "—"} h={!ok ? "carregando" : t.kpis.itens ? `de ${t.kpis.fornecedores} fornecedor(es)` : "nada pendente"} />
        <Kpi k="Valor estimado" v={ok ? brl(t.kpis.valor) : "—"} h={t.kpis.semPreco ? "há itens sem preço" : "pelo último preço pago"} />
        <Kpi k="Pedidos feitos" v={ok ? String(t.kpis.pedidos) : "—"} h="aguardando entrada" />
        <Kpi k="Eventos no período" v={ok ? String(t.kpis.eventos) : "—"} h="contratados ou em produção" />
      </div>
      {!ok || !t.fornecedores.length ? (
        <section className="panel">{!ok ? <Carregando erro={erro} /> : <Vazio titulo="Nada a comprar">O estoque cobre os eventos do período e os mínimos.</Vazio>}</section>
      ) : (
        t.fornecedores.map((f) => {
          const txt = `PEDIDO · ${f.fornecedor} · ${fdate(hoje())}\n${f.itens
            .filter((x) => !x.pedido)
            .map((x) => `- ${x.nome}: ${x.embalagens ? `${comprarTxt(x)} (${qtdU(x.comprar, x.unidade)})` : comprarTxt(x)}`)
            .join("\n")}\n`;
          return (
            <section key={f.fornecedor} className="panel">
              <div className="panel-h">
                <h3>{f.fornecedor}</h3>
                <span className="note">{brl(f.valor)}</span>
              </div>
              <div className="tablewrap">
                <table>
                  <thead>
                    <tr>
                      <th>Item</th>
                      <th className="r">Saldo</th>
                      <th className="r">Eventos</th>
                      <th className="r">Mínimo</th>
                      <th className="r">Comprar</th>
                      <th className="r">Valor</th>
                      <th>Pedido</th>
                    </tr>
                  </thead>
                  <tbody>
                    {f.itens.map((x) => (
                      <tr key={x.insumoId} className={x.pedido ? "off" : ""}>
                        <td className="wrapc">
                          <button type="button" className="linkish" onClick={() => abrir(x.nome, <ItemSheet id={x.insumoId} />)}>
                            <b>{x.nome}</b>
                          </button>
                          {x.eventos.length > 0 && (
                            <>
                              <br />
                              <span className="note">{x.eventos.join(", ")}</span>
                            </>
                          )}
                        </td>
                        <td className={`r${x.saldo < 0 ? " neg" : ""}`}>
                          {x.saldo < 0 ? "−" : ""}
                          {qtdU(x.saldo, x.unidade)}
                        </td>
                        <td className="r">{x.reservado ? qtdU(x.reservado, x.unidade) : "—"}</td>
                        <td className="r">{x.minimo ? qtdU(x.minimo, x.unidade) : "—"}</td>
                        <td className="r">
                          <b>{comprarTxt(x)}</b>
                          {x.embalagens > 0 && (
                            <>
                              <br />
                              <span className="note">{qtdU(x.comprar, x.unidade)}</span>
                            </>
                          )}
                        </td>
                        <td className="r">{x.semPreco ? <span className="warn-t">sem preço</span> : brl(x.valor)}</td>
                        <td>
                          <label className="check" style={{ fontSize: 13, fontWeight: 500 }}>
                            <input type="checkbox" checked={x.pedido} onChange={(e) => pedido(x.insumoId, e.target.checked)} />{" "}
                            {x.pedido && x.pedidoEm ? fdate(x.pedidoEm.slice(0, 10)) : "feito"}
                          </label>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div style={{ padding: "10px 16px" }}>
                <CopiarTexto texto={txt} nota="Para colar no WhatsApp do fornecedor." />
              </div>
            </section>
          );
        })
      )}
    </>
  );
}

const quando = (d: number) => (d < 0 ? `venceu há ${-d}d` : d === 0 ? "vence hoje" : `em ${d}d`);

export function AbaValidades() {
  const { abrir } = useSheet();
  const { dados, erro } = useDados(telaValidades);
  const ok = dados != null;
  const t = dados ?? normalizarValidades(null);
  const k = t.kpis;
  const linha = (x: ValidadeItemDTO, ix: number) => {
    const tom = x.dias < 0 ? "bad" : x.dias <= 1 ? "warn" : "";
    if (x.tipo === "lote" && x.loteId != null)
      return (
        <button key={ix} type="button" className="row" onClick={() => abrir("Lote", <LoteSheet id={x.loteId!} />)}>
          <Datebox iso={x.validade} />
          <span className="grow">
            <span className="t">{x.nome}</span>
            <span className="m">{x.detalhe}</span>
          </span>
          <Pill tom={tom}>{quando(x.dias)}</Pill>
        </button>
      );
    return (
      <div key={ix} className="row static">
        <Datebox iso={x.validade} />
        <button type="button" className="grow linkish" onClick={() => x.insumoId && abrir(x.nome, <ItemSheet id={x.insumoId} />)}>
          <span className="t">{x.nome}</span>
          <span className="m">{x.detalhe}</span>
        </button>
        <Pill tom={tom}>{quando(x.dias)}</Pill>
        {x.insumoId != null && (
          <button type="button" className="btn sm" onClick={() => abrir("Lançar movimento", <MovForm tipo="perda" insumoId={x.insumoId!} qtd={x.qtd} motivo="Vencido" />)}>
            Descartar
          </button>
        )}
      </div>
    );
  };
  return (
    <>
      <p className="note">
        Pré-preparos pela validade do lote; insumos pela validade informada na entrada (campo Validade em Lançar movimento › Entrada). O que entrou primeiro é considerado usado
        primeiro.
      </p>
      <div className="kpis">
        <Kpi k="Vencidos com saldo" v={ok ? String(k.vencidos) : "—"} negativo={k.vencidos > 0} h={!ok ? "carregando" : k.vencidos ? "descarte e registre a perda" : "nenhum"} />
        <Kpi k="Vencem em 7 dias" v={ok ? String(k.vencem) : "—"} h="use primeiro" />
        <Kpi k="Valor em risco" v={ok ? brl(k.valorRisco) : "—"} h="pelo custo atual" />
        <Kpi k="Insumos sem validade" v={ok ? String(k.semValidade) : "—"} h="com saldo e sem data" />
      </div>
      <section className="panel">
        <div className="panel-b">
          {!ok ? (
            <Carregando erro={erro} />
          ) : t.itens.length ? (
            t.itens.map(linha)
          ) : (
            <Vazio titulo="Nada vencido nem vencendo nos próximos 7 dias">Informe a validade nas entradas de perecíveis para acompanhar aqui.</Vazio>
          )}
        </div>
      </section>
    </>
  );
}

export function AbaPerdas() {
  const { abrir } = useSheet();
  const [mes, setMes] = useState(hoje().slice(0, 7));
  const { dados, erro } = useDados(() => telaPerdas(mes), [mes]);
  const ok = dados != null;
  const t = dados ?? normalizarPerdas(null, mes);
  // Os três grupos aparecem sempre; os que vierem a mais do backend entram depois.
  const grupos = [...GRUPOS_PERDA, ...t.porGrupo.map((g) => g.grupo).filter((g) => !(GRUPOS_PERDA as readonly string[]).includes(g))];
  const valorGrupo = (g: string) => t.porGrupo.find((x) => x.grupo === g)?.valor ?? 0;
  return (
    <>
      <div className="toolbar">
        <div className="monthnav">
          <button type="button" className="btn sm" aria-label="Mês anterior" onClick={() => setMes(addMonths(`${mes}-01`, -1).slice(0, 7))}>
            ‹
          </button>
          <b>{ymLabel(mes)}</b>
          <button type="button" className="btn sm" aria-label="Próximo mês" onClick={() => setMes(addMonths(`${mes}-01`, 1).slice(0, 7))}>
            ›
          </button>
        </div>
      </div>
      <div className="kpis">
        <Kpi k="Perdas no mês" v={ok ? brl(t.kpis.total) : "—"} negativo={t.kpis.total > 0} h={`${t.kpis.registros} registro${t.kpis.registros === 1 ? "" : "s"}`} />
        <Kpi k="% das compras" v={ok ? pct(t.kpis.pctCompras) : "—"} h={`entradas do mês: ${brl(t.kpis.comprasMes)}`} />
        <Kpi k="Sobras que voltaram" v={ok ? brl(t.kpis.sobras) : "—"} h={`${t.kpis.sobrasRegistros} registro${t.kpis.sobrasRegistros === 1 ? "" : "s"}`} />
        <Kpi k="Maior causa" v={t.kpis.maiorCausa ?? "—"} h="pelo valor" />
      </div>
      <div className="summary">
        {grupos.map((g) => (
          <span key={g}>
            {g}: <b className="num">{ok ? brl(valorGrupo(g)) : "—"}</b>
          </span>
        ))}
      </div>
      <section className="panel">
        <div className="panel-b">
          {!ok ? (
            <Carregando erro={erro} />
          ) : t.perdas.length ? (
            t.perdas.map((x, ix) => (
              <button
                key={ix}
                type="button"
                className="row"
                onClick={() => (x.loteId != null ? abrir("Lote", <LoteSheet id={x.loteId} />) : x.insumoId != null ? abrir(x.nome, <ItemSheet id={x.insumoId} />) : undefined)}
              >
                <Datebox iso={x.data} />
                <span className="grow">
                  <span className="t">{x.nome}</span>
                  <span className="m">{[x.grupo, x.motivo, x.eventoNome].filter(Boolean).join(" · ")}</span>
                </span>
                <span className="m">
                  {fmtN(x.qtd)} {x.unidade}
                </span>
                <span className="amt neg">{brl(x.valor)}</span>
              </button>
            ))
          ) : (
            <Vazio titulo={`Nenhuma perda em ${ymLabel(mes)}`}>Perdas, descartes de lote e avarias de louça aparecem aqui com o valor.</Vazio>
          )}
        </div>
      </section>
    </>
  );
}

export function AbaConferencia() {
  const { abrir } = useSheet();
  const { dados, erro } = useDados(telaConferencia);
  const ok = dados != null;
  const t = dados ?? normalizarConferencia(null);
  return (
    <>
      <p className="note">
        Regras de integridade: toda movimentação tem origem; cada operação baixa o estoque uma vez só. A baixa de produção vem do checklist do evento, da ordem de produção
        conferida ou do lote de pré-preparo, nunca de dois deles: quando um lote do estoque é usado num evento, essa quantidade é abatida da baixa do evento.
      </p>
      <section className="panel">
        <div className="panel-h">
          <h3>Movimentos por origem</h3>
        </div>
        <div className="panel-b">
          {!ok ? (
            <Carregando erro={erro} />
          ) : t.porOrigem.length ? (
            t.porOrigem.map((o) => (
              <div key={o.origem} className="row static">
                <span className="grow">{o.origem}</span>
                <span className="amt">{o.n}</span>
              </div>
            ))
          ) : (
            <Vazio titulo="Nenhum movimento registrado">Entradas, notas, inventários, consumos e baixas de produção aparecem aqui pela origem.</Vazio>
          )}
        </div>
      </section>
      <section className="panel">
        <div className="panel-h">
          <h3>Pontos para conferir</h3>
          <Pill tom={!ok ? "" : t.pontos.length ? "warn" : "good"}>{ok ? t.pontos.length : "—"}</Pill>
        </div>
        <div className="panel-b">
          {!ok ? (
            <Carregando erro={erro} />
          ) : t.pontos.length ? (
            t.pontos.map((x, ix) => {
              const abrirPonto =
                x.insumoId != null
                  ? () => abrir("Item", <ItemSheet id={x.insumoId!} />)
                  : x.loteId != null
                    ? () => abrir("Lote", <LoteSheet id={x.loteId!} />)
                    : x.nfId != null
                      ? async () => abrir("Nota fiscal", <NfSheet nf={await obterNf(x.nfId!)} />, { larga: true })
                      : null;
              const corpo = (
                <>
                  <span className="dot" />
                  <span className="grow wrap">{x.texto}</span>
                </>
              );
              return abrirPonto ? (
                <button key={ix} type="button" className={`row alert ${x.tom}`} onClick={abrirPonto}>
                  {corpo}
                </button>
              ) : (
                <div key={ix} className={`row alert ${x.tom}`}>
                  {corpo}
                </div>
              );
            })
          ) : (
            <Vazio titulo="Nada a conferir">Sem saldos negativos, duplicidades ou notas incompletas.</Vazio>
          )}
        </div>
      </section>
    </>
  );
}
