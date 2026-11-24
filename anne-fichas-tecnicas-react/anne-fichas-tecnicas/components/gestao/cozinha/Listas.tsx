"use client";

import { useState } from "react";
import { Chips, CopiarTexto, Pill, Vazio } from "@/components/ui/Gestao";
import type { CozinhaProjecaoDTO, ExecucaoResponseDTO, PratoProjecaoDTO } from "@/lib/models/cozinha";
import { CATEGORIAS_INSUMO } from "@/lib/models/dominio";
import { fdate, fmtN } from "@/lib/utils/numero";
import { addDays } from "@/lib/utils/datas";
import { statusProducao } from "@/lib/utils/producao";
import { catPrepLabel, consLabel, exDe, ordemCat, ordemPraca, qU, tomFicha, useCozinha, useSalvarExec } from "./comum";

/** 1 · Lista de produção: quanto produzir de cada prato. */
export function AbaProducao({ pj }: { pj: CozinhaProjecaoDTO }) {
  const { dados } = useCozinha();
  const salvar = useSalvarExec();
  // Comprados prontos não são produzidos: ficam numa lista própria de retirada do estoque.
  const prontos = pj.pratos.filter((x) => x.pronto && x.st !== "nao");
  const ps = pj.pratos.filter((x) => !x.pronto).sort((a, b) => ordemCat(a.cat) - ordemCat(b.cat) || a.nome.localeCompare(b.nome, "pt-BR"));
  const sim = ps.filter((x) => x.st === "sim").length;
  const def = ps.filter((x) => x.st === "definir").length;
  const ativos = ps.filter((x) => x.st !== "nao");
  const txt = textoProducao(pj, ps);
  return (
    <>
      <p className="note">
        A produzir = porção por convidado × {pj.N} pessoas (convidados + margem). Pratos <b>a definir</b> ainda não entram em compras nem no checklist.
      </p>
      <div className="kpis">
        <div className="kpi">
          <span className="k">Confirmados</span>
          <span className="v">{sim}</span>
          <span className="h">pratos Sim</span>
        </div>
        <div className="kpi">
          <span className="k">A definir</span>
          <span className="v">{def}</span>
          <span className="h">aguardando o cliente</span>
        </div>
        <div className="kpi">
          <span className="k">Fichas completas</span>
          <span className="v">
            {ativos.filter((x) => x.ficha === "Completa").length}/{ativos.length}
          </span>
          <span className="h">com insumos e preço</span>
        </div>
        <div className="kpi">
          <span className="k">Produzidos</span>
          <span className="v">
            {ps.filter((x) => x.st === "sim" && (exDe(dados, `i_${x.cid}`).produzido ?? 0) > 0).length}/{sim}
          </span>
          <span className="h">pelo checklist</span>
        </div>
      </div>
      {!ps.length ? (
        <section className="panel">
          <Vazio titulo="Cardápio vazio">A gestão ainda não incluiu os pratos deste evento.</Vazio>
        </section>
      ) : (
        <section className="panel">
          <div className="tablewrap">
            <table className="cztable">
              <thead>
                <tr>
                  <th>Prato</th>
                  <th>Praça</th>
                  <th>No cardápio</th>
                  <th className="r">Porção / pessoa</th>
                  <th className="r">A produzir</th>
                  <th className="r">Receitas</th>
                  <th>Ficha</th>
                  <th>Resp.</th>
                  <th>Início</th>
                  <th>Conclusão</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {ps.map((x, i) => {
                  const off = x.st === "nao";
                  const cab = i === 0 || ps[i - 1]!.cat !== x.cat;
                  return [
                    cab && (
                      <tr key={`g${x.cid}`} className="grp-row">
                        <td colSpan={11}>{catPrepLabel(x.cat)}</td>
                      </tr>
                    ),
                    <tr key={x.cid} className={off ? "off" : ""}>
                      <td className="wrapc">
                        <b>{x.nome}</b>
                      </td>
                      <td>{x.praca}</td>
                      <td>
                        <Pill tom={x.st === "sim" ? "accent" : x.st === "definir" ? "warn" : ""}>{x.st === "sim" ? "Sim" : x.st === "definir" ? "A definir" : "Não"}</Pill>
                      </td>
                      <td className="r">{x.fator.replace(" por convidado", "")}</td>
                      <td className="r">
                        <b>{off ? "—" : qU(x.total, x.uu)}</b>
                      </td>
                      <td className="r">{off || x.rend === 1 ? "—" : `${fmtN(Math.round(x.lotes * 100) / 100)}×`}</td>
                      <td>
                        <Pill tom={tomFicha(x.ficha)}>{x.ficha}</Pill>
                      </td>
                      <td>{x.resp || "—"}</td>
                      {off ? (
                        <>
                          <td />
                          <td />
                          <td />
                        </>
                      ) : (
                        <Horarios pj={pj} x={x} ex={exDe(dados, `i_${x.cid}`)} salvar={salvar} />
                      )}
                    </tr>,
                  ];
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {prontos.length > 0 && (
        <section className="panel">
          <div className="panel-h">
            <h3>Comprados prontos · retirar do estoque</h3>
            <span className="note">a produção não faz</span>
          </div>
          <div className="tablewrap">
            <table className="cztable">
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Fornecedor</th>
                  <th>No cardápio</th>
                  <th className="r">Quantidade</th>
                  <th>Retirar em</th>
                  <th>Finalização</th>
                </tr>
              </thead>
              <tbody>
                {prontos.map((x) => (
                  <tr key={x.cid}>
                    <td className="wrapc">
                      <b>{x.nome}</b>
                    </td>
                    <td>{x.forn || "—"}</td>
                    <td>
                      <Pill tom={x.st === "sim" ? "accent" : "warn"}>{x.st === "sim" ? "Sim" : "A definir"}</Pill>
                    </td>
                    <td className="r">
                      <b>{qU(x.total, x.uu)}</b>
                    </td>
                    <td>{pj.data && x.dnf != null ? fdate(addDays(pj.data, -x.dnf)) : "—"}</td>
                    <td className="wrapc">{x.obsEnvio || "Finalizar conforme a ficha"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      <CopiarTexto texto={txt} rotulo="Copiar como texto" nota="Para colar no WhatsApp da equipe ou do fornecedor." />
    </>
  );
}

/** Início, conclusão e situação de um prato na lista de produção (salvam ao sair do campo). */
function Horarios({ pj, x, ex, salvar }: { pj: CozinhaProjecaoDTO; x: PratoProjecaoDTO; ex: Partial<ExecucaoResponseDTO>; salvar: ReturnType<typeof useSalvarExec> }) {
  const st = statusProducao(pj, x, ex);
  const campo = (k: "inicio" | "fim", rot: string) => (
    <td>
      <input
        className="ckin"
        type="time"
        defaultValue={ex[k] ?? ""}
        aria-label={`${rot} de ${x.nome}`}
        onBlur={(e) => e.target.value !== (ex[k] ?? "") && salvar({ chave: `i_${x.cid}`, kind: "item", eventoId: pj.eventoId, cardapioId: x.cid, campos: { [k]: e.target.value } })}
      />
    </td>
  );
  return (
    <>
      {campo("inicio", "Início")}
      {campo("fim", "Conclusão")}
      <td>
        <Pill tom={st[0]}>{st[1]}</Pill>
      </td>
    </>
  );
}

/** 2 · Insumos por prato, agrupados por praça, com sub-receitas abertas. */
export function AbaInsumos({ pj }: { pj: CozinhaProjecaoDTO }) {
  const ps = pj.pratos.filter((x) => x.st !== "nao").sort((a, b) => ordemPraca(a.praca) - ordemPraca(b.praca) || ordemCat(a.cat) - ordemCat(b.cat) || a.nome.localeCompare(b.nome, "pt-BR"));
  const txt = textoInsumos(pj, ps);
  return (
    <>
      <p className="note">Cada card lê a ficha técnica e multiplica pela quantidade do evento. Sub-receitas (massa, recheio, molho) já aparecem abertas nos seus ingredientes, com &quot;via&quot;. Agrupado por praça.</p>
      {!ps.length && (
        <section className="panel">
          <Vazio titulo="Nenhum prato no cardápio" />
        </section>
      )}
      {ps.map((x, i) => {
        const cab = i === 0 || ps[i - 1]!.praca !== x.praca;
        return (
          <div key={x.cid} style={{ display: "contents" }}>
            {cab && <p className="grp">{x.praca}</p>}
            <section className="panel czcard">
              <div className="panel-h" style={{ alignItems: "flex-start" }}>
                <div style={{ minWidth: 0 }}>
                  <span className="note" style={{ fontSize: 12 }}>
                    {catPrepLabel(x.cat)} · {consLabel(x.conserv)}
                  </span>
                  <div style={{ fontWeight: 700 }}>
                    {x.nome} {x.st === "definir" && <Pill tom="warn">a definir</Pill>}
                  </div>
                  <span className="note">
                    A produzir <b>{qU(x.total, x.uu)}</b> · ficha rende {x.rendLabel}
                    {x.rend !== 1 ? ` · ${fmtN(Math.round(x.lotes * 100) / 100)}× a receita` : ""}
                    {x.resp ? ` · resp. ${x.resp}` : ""}
                  </span>
                </div>
                <Pill tom={tomFicha(x.ficha)}>{x.ficha}</Pill>
              </div>
              {x.ins.length ? (
                <div className="tablewrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Insumo</th>
                        <th className="r">Por receita ({x.rendLabel})</th>
                        <th className="r">Total do evento</th>
                      </tr>
                    </thead>
                    <tbody>
                      {x.ins.map((it) => (
                        <tr key={it.k}>
                          <td className="wrapc">
                            {it.n}
                            {it.via && (
                              <>
                                <br />
                                <span className="note">via {it.via}</span>
                              </>
                            )}
                          </td>
                          <td className="r">{qU(it.qb, it.u)}</td>
                          <td className="r">
                            <b>{qU(it.qt, it.u)}</b>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <Vazio>Ficha sem insumos cadastrados: peça à gestão para completar.</Vazio>
              )}
              {(x.modo || x.etapas.length > 0 || x.loucas.length > 0) && (
                <details className="czdet">
                  <summary>Modo de preparo{x.loucas.length ? ", louças" : ""}</summary>
                  {x.modo && <p style={{ whiteSpace: "pre-wrap", margin: "0 0 8px" }}>{x.modo}</p>}
                  {x.etapas.length > 0 && (
                    <ol style={{ margin: "0 0 8px", paddingLeft: 20 }}>
                      {x.etapas.map((t, j) => (
                        <li key={j}>
                          {t.desc} {t.dn >= 0 && <span className="note">(D-{t.dn})</span>}
                        </li>
                      ))}
                    </ol>
                  )}
                  {x.loucas.length > 0 && <p className="note">Louças: {x.loucas.map((l) => `${fmtN(l.q)}× ${l.n}`).join(", ")}</p>}
                </details>
              )}
            </section>
          </div>
        );
      })}
      <CopiarTexto texto={txt} rotulo="Copiar como texto" nota="Para colar no WhatsApp da equipe ou do fornecedor." />
    </>
  );
}

export const FILTROS_COMPRA = [
  ["comprar", "A comprar"],
  ["comprados", "Comprados"],
  ["todos", "Todos"],
] as const;
export type FiltroCompra = (typeof FILTROS_COMPRA)[number][0];

/** Linhas da lista de compras com o que falta e o que já foi marcado. */
export function linhasCompra(pj: CozinhaProjecaoDTO, marcados: Record<string, boolean>, f: FiltroCompra) {
  return pj.compras
    .map((x) => ({ ...x, falta: Math.round(Math.max(0, x.q - x.est) * 1000) / 1000, ok: !!marcados[String(x.id)] }))
    .filter((x) => (f === "todos" ? true : f === "comprados" ? x.ok : !x.ok && (x.falta > 0 || x.qd > 0)));
}
export const embalagens = (x: { fc: number; falta: number; uc: string }) => (x.fc > 0 && x.falta > 0 ? `${Math.ceil(x.falta / x.fc - 1e-9)}× ${x.uc || "emb."}` : "");

/** 3 · Lista de compras: confirmados − em estoque, por setor, com marcação de comprado. */
export function AbaCompras({ pj, filtro, setFiltro }: { pj: CozinhaProjecaoDTO; filtro: FiltroCompra; setFiltro: (f: FiltroCompra) => void }) {
  const { dados } = useCozinha();
  const salvar = useSalvarExec();
  const marc = exDe(dados, `cp_${pj.eventoId}`).itens ?? {};
  const vis = linhasCompra(pj, marc, filtro);
  const txt = textoCompras(pj, vis);
  function marcar(id: number, v: boolean) {
    const it = { ...marc };
    if (v) it[String(id)] = true;
    else delete it[String(id)];
    salvar({ chave: `cp_${pj.eventoId}`, kind: "compras", eventoId: pj.eventoId, campos: { itens: it } });
  }
  return (
    <>
      <p className="note">
        Soma dos insumos de todos os pratos, já com a margem. <b>Em estoque</b> = saldo físico menos o reservado para outros eventos. <b>Comprar</b> = confirmados − em estoque. Pratos a definir aparecem à parte e não entram no Comprar.
      </p>
      <Chips opcoes={FILTROS_COMPRA} valor={filtro} onChange={setFiltro} rotulo="Filtro de compras" />
      {!pj.compras.length ? (
        <section className="panel">
          <Vazio titulo="Nada a comprar ainda">Os pratos confirmados precisam ter ficha técnica com ingredientes.</Vazio>
        </section>
      ) : (
        CATEGORIAS_INSUMO.map((g) => {
          const its = vis.filter((x) => ((CATEGORIAS_INSUMO as readonly string[]).includes(x.cat) ? x.cat : "Outros") === g).sort((a, b) => a.n.localeCompare(b.n, "pt-BR"));
          if (!its.length) return null;
          return (
            <div key={g} style={{ display: "contents" }}>
              <p className="grp">{g}</p>
              <section className="panel">
                <div className="tablewrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Insumo</th>
                        <th className="r">Confirm.</th>
                        <th className="r">A definir</th>
                        <th className="r">Em estoque</th>
                        <th className="r">Comprar</th>
                        <th>Comprado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {its.map((x) => (
                        <tr key={x.id} className={x.ok ? "off" : ""}>
                          <td className="wrapc">
                            <b>{x.n}</b>
                            <br />
                            <span className="note">{x.usos}</span>
                          </td>
                          <td className="r">{x.q ? qU(x.q, x.u) : "—"}</td>
                          <td className="r">{x.qd ? <span className="warn-t">{qU(x.qd, x.u)}</span> : "—"}</td>
                          <td className="r">{qU(x.est, x.u)}</td>
                          <td className="r">
                            <b>{x.falta > 0 ? qU(x.falta, x.u) : "—"}</b>
                            {embalagens(x) && (
                              <>
                                <br />
                                <span className="note">{embalagens(x)}</span>
                              </>
                            )}
                          </td>
                          <td>
                            <input type="checkbox" className="czck" aria-label={`Comprado: ${x.n}`} checked={x.ok} onChange={(e) => marcar(x.id, e.target.checked)} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            </div>
          );
        })
      )}
      {pj.compras.length > 0 && !vis.length && (
        <section className="panel">
          {filtro === "comprar" ? <Vazio titulo="Nada falta comprar">O estoque cobre os pratos confirmados, ou tudo já foi marcado como comprado.</Vazio> : <Vazio>Nenhum item neste filtro.</Vazio>}
        </section>
      )}
      <p className="note">Ao receber a mercadoria, a entrada é feita pela nota fiscal em Estoque › Notas (gestão).</p>
      <CopiarTexto texto={txt} rotulo="Copiar como texto" nota="Para colar no WhatsApp da equipe ou do fornecedor." />
    </>
  );
}

export function useFiltroCompra() {
  return useState<FiltroCompra>("comprar");
}

/* ---------------------- Textos para copiar (WhatsApp) ---------------------- */

const dataBR = (iso: string) => iso.split("-").reverse().join("/");

function textoProducao(pj: CozinhaProjecaoDTO, ps: CozinhaProjecaoDTO["pratos"]): string {
  let t = `LISTA DE PRODUÇÃO · ${pj.codigo} · ${pj.nome} · ${dataBR(pj.data)} · ${pj.N} pessoas\n`;
  ps.forEach((x, i) => {
    if (i === 0 || ps[i - 1]!.cat !== x.cat) t += `\n${catPrepLabel(x.cat).toUpperCase()}\n`;
    if (x.st !== "nao") t += `- ${x.st === "definir" ? "[A DEFINIR] " : ""}${x.nome}: ${qU(x.total, x.uu)}${x.resp ? ` · ${x.resp}` : ""}\n`;
  });
  return t;
}

function textoInsumos(pj: CozinhaProjecaoDTO, ps: CozinhaProjecaoDTO["pratos"]): string {
  let t = `INSUMOS POR PRATO · ${pj.codigo} · ${pj.nome} · ${pj.N} pessoas\n`;
  ps.forEach((x, i) => {
    if (i === 0 || ps[i - 1]!.praca !== x.praca) t += `\n${x.praca.toUpperCase()}\n`;
    t += `\n${x.st === "definir" ? "[A DEFINIR] " : ""}${x.nome} — ${qU(x.total, x.uu)}\n`;
    for (const it of x.ins) t += `  ${it.n}${it.via ? ` (via ${it.via})` : ""}: ${qU(it.qt, it.u)}\n`;
  });
  return t;
}

function textoCompras(pj: CozinhaProjecaoDTO, vis: ReturnType<typeof linhasCompra>): string {
  let t = `LISTA DE COMPRAS · ${pj.codigo} · ${pj.nome} · ${dataBR(pj.data)}\n`;
  for (const g of CATEGORIAS_INSUMO) {
    const tg = vis
      .filter((x) => ((CATEGORIAS_INSUMO as readonly string[]).includes(x.cat) ? x.cat : "Outros") === g && !x.ok && x.falta > 0)
      .sort((a, b) => a.n.localeCompare(b.n, "pt-BR"))
      .map((x) => `- ${x.n}: ${qU(x.falta, x.u)}${embalagens(x) ? ` (${embalagens(x)})` : ""}\n`)
      .join("");
    if (tg) t += `\n${g.toUpperCase()}\n${tg}`;
  }
  return t;
}
