import type { CozinhaProjecaoDTO, CozinhaResponseDTO, ExecucaoResponseDTO, OrdemProducaoDTO, TarefaCozinhaDTO } from "@/lib/models/cozinha";
import { CATEGORIAS_INSUMO, ETAPAS_ORDEM, PRACAS, SETORES_PADRAO, dnLabel, rotulo } from "@/lib/models/dominio";
import { CZ_REGRAS, CZ_SECS, czEnvV, czFria, czRows } from "@/lib/utils/checklist";
import { addDays, hoje, segunda } from "@/lib/utils/datas";
import { dLabel, fdate, fmtN, fmtQ } from "@/lib/utils/numero";
import { embalagens, linhasCompra, type FiltroCompra } from "./Listas";
import { catPrepLabel, consLabel, exDe, ordemCat, qU } from "./comum";
import { abrirDocumento, docA4Html, esc } from "@/lib/utils/documento";

/**
 * Impressão da Cozinha: um documento A4 próprio para cada aba (modelo
 * "Cronograma da semana" do protótipo), aberto em outra janela para imprimir
 * ou salvar em PDF. Só monta HTML a partir do que o BFF já entregou.
 */

export type AbaCozinha = "lider" | "ordens" | "agenda" | "producao" | "insumos" | "compras" | "cronograma" | "checklist" | "ocorrencias" | "indicadores";

export interface OpcoesImpressao {
  aba: AbaCozinha;
  pj: CozinhaProjecaoDTO | undefined;
  praca: string;
  semana: string;
  filtroCompra: FiltroCompra;
  /** Aba Ordens: imprime só o setor filtrado. */
  setor?: string;
}


const DIAS_FULL = ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"];
const dFull = (iso: string) => `${DIAS_FULL[new Date(iso + "T12:00:00").getDay()]} ${iso.slice(8, 10)}/${iso.slice(5, 7)}`;
const pBox = '<span class="bx"></span>';
const pTick = '<span class="bx on">✓</span>';


const czDoc = docA4Html;

const czBrand = (pj: CozinhaProjecaoDTO) => `Anne Buffet · ${pj.nome} · ${dFull(pj.data)}/${pj.data.slice(0, 4)}`;
const czObs = (pj: CozinhaProjecaoDTO) =>
  (!pj.fechado ? '<p class="warn"><b>Cardápio ainda em montagem</b>: quantidades podem mudar. Produza só o que está confirmado.</p>' : "") +
  (pj.obs ? `<div class="obs"><b>Observações e restrições</b>\n${esc(pj.obs)}</div>` : "");
const ordPraca = (p: string) => PRACAS.indexOf(p as (typeof PRACAS)[number]);
const ordT = (a: TarefaCozinhaDTO, b: TarefaCozinhaDTO) => ordPraca(a.praca) - ordPraca(b.praca) || String(a.produtos || "").localeCompare(String(b.produtos || ""), "pt-BR");
const pl = (n: number, um: string, varios: string) => `${n} ${n === 1 ? um : varios}`;
const badgePara = (pj: CozinhaProjecaoDTO): [string, string] => ["Produzir para", `${pj.N} pessoas`];
const receitas = (lotes: number) => `${fmtN(Math.round(lotes * 100) / 100)}×`;

function tabTarefas(list: TarefaCozinhaDTO[], comEvento: boolean): string {
  return (
    `<table><thead><tr><th class="c" style="width:11mm">Feito</th>${comEvento ? '<th style="width:24mm">Evento</th>' : ""}<th style="width:22mm">Praça</th><th>Tarefa</th><th style="width:${comEvento ? "38" : "46"}mm">Produtos</th><th style="width:30mm">Responsável</th></tr></thead><tbody>` +
    list
      .map(
        (t) =>
          `<tr><td class="c">${t.feito ? pTick : pBox}</td>${comEvento ? `<td><span class="ev">${esc(t.codigo)}</span><br><span class="pd" style="font-size:7.5pt;color:#7a776f">${esc(t.eventoNome)}</span></td>` : ""}<td class="pr">${esc(t.praca)}</td><td>${esc(t.desc)}${t.padrao && !t.movida ? '<span class="tag">prazo padrão</span>' : ""}</td><td class="pd">${esc(t.produtos || "")}</td><td>${esc(t.resp || "")}</td></tr>`
      )
      .join("") +
    "</tbody></table>"
  );
}

function printCrono(dados: CozinhaResponseDTO, pj: CozinhaProjecaoDTO, praca: string): string {
  const ts = dados.tarefas.filter((t) => t.eventoId === pj.eventoId && (!praca || t.praca === praca));
  let body = czObs(pj);
  const dias = [...new Set(ts.filter((t) => t.data).map((t) => t.data!))].sort();
  for (const d of dias) {
    const l = ts.filter((t) => t.data === d).sort(ordT);
    body += `<h2>${esc(dFull(d))}${d === pj.data ? " · dia do evento" : ""}<small>${pl(l.length, "tarefa", "tarefas")}</small></h2>${tabTarefas(l, false)}`;
  }
  const sem = ts.filter((t) => !t.data).sort(ordT);
  if (sem.length) body += `<h2>A posicionar<small>${sem.length} sem dia definido</small></h2>${tabTarefas(sem, false)}`;
  if (!ts.length) body += '<p class="empty">Nenhuma tarefa.</p>';
  return czDoc({ titulo: "Cronograma de produção", brand: czBrand(pj), sub: `Sequência sugerida${pj.equipe ? ` · equipe: ${pj.equipe}` : ""}${praca ? ` · praça: ${praca}` : ""}`, badge: badgePara(pj), body });
}

function printAgenda(dados: CozinhaResponseDTO, semana: string, praca: string): string {
  const ini = semana || segunda(hoje());
  const fim = addDays(ini, 6);
  const all = dados.tarefas.filter((x) => x.data && x.data >= ini && x.data <= fim && (!praca || x.praca === praca));
  let body = "";
  for (let i = 0; i < 7; i++) {
    const d = addDays(ini, i);
    const evs = dados.projecoes.filter((p) => p.data === d);
    const l = all.filter((x) => x.data === d).sort((a, b) => a.eventoData.localeCompare(b.eventoData) || ordT(a, b));
    if (!l.length && !evs.length) continue;
    body +=
      `<h2>${esc(dFull(d))}<small>${pl(l.length, "tarefa", "tarefas")}${evs.length ? ` · evento: ${esc(evs.map((p) => `${p.codigo} ${p.nome} (${p.N} pessoas)`).join(", "))}` : ""}</small></h2>` +
      (l.length ? tabTarefas(l, true) : '<p class="note">Sem tarefas de produção neste dia.</p>');
  }
  if (!body) body = '<p class="empty">Nenhuma tarefa nesta semana.</p>';
  return czDoc({ titulo: "Agenda da cozinha", brand: `Anne Buffet · semana de ${dLabel(ini)} a ${dLabel(fim)}`, sub: `Todas as tarefas de produção dos eventos${praca ? ` · praça: ${praca}` : ""}`, badge: ["Tarefas na semana", String(all.length)], body });
}

function printProducao(dados: CozinhaResponseDTO, pj: CozinhaProjecaoDTO): string {
  const ps = pj.pratos.filter((x) => x.st !== "nao").sort((a, b) => ordemCat(a.cat) - ordemCat(b.cat) || a.nome.localeCompare(b.nome, "pt-BR"));
  let body = czObs(pj);
  for (const c of [...new Set(ps.map((x) => x.cat))]) {
    const l = ps.filter((x) => x.cat === c);
    body +=
      `<h2>${esc(catPrepLabel(c))}<small>${pl(l.length, "prato", "pratos")}</small></h2><table><thead><tr><th class="c" style="width:11mm">Feito</th><th>Prato</th><th style="width:24mm">Praça</th><th class="r" style="width:24mm">Porção/pessoa</th><th class="r" style="width:24mm">A produzir</th><th class="r" style="width:16mm">Receitas</th><th style="width:30mm">Responsável</th></tr></thead><tbody>` +
      l
        .map(
          (x) =>
            `<tr><td class="c">${(Number(exDe(dados, `i_${x.cid}`).produzido) || 0) > 0 ? pTick : pBox}</td><td><b>${esc(x.nome)}</b>${x.st === "definir" ? '<span class="tag">a definir</span>' : ""}${x.ficha === "Incompleta" ? '<span class="tag">ficha incompleta</span>' : ""}</td><td class="pr">${esc(x.praca)}</td><td class="r">${esc(x.fator.replace(" por convidado", ""))}</td><td class="r big">${esc(qU(x.total, x.uu))}</td><td class="r">${x.rend === 1 ? "—" : receitas(x.lotes)}</td><td>${esc(x.resp || "")}</td></tr>`
        )
        .join("") +
      "</tbody></table>";
  }
  if (!ps.length) body += '<p class="empty">Cardápio vazio.</p>';
  return czDoc({ titulo: "Lista de produção", brand: czBrand(pj), sub: `A produzir = porção por pessoa × ${pj.N} pessoas (${pj.base} + ${fmtN(pj.margem)}% de margem)`, badge: badgePara(pj), body });
}

function printInsumos(pj: CozinhaProjecaoDTO): string {
  const ps = pj.pratos.filter((x) => x.st !== "nao").sort((a, b) => ordPraca(a.praca) - ordPraca(b.praca) || ordemCat(a.cat) - ordemCat(b.cat) || a.nome.localeCompare(b.nome, "pt-BR"));
  let body = czObs(pj);
  let lp = "";
  for (const x of ps) {
    if (x.praca !== lp) {
      body += `<div class="grp">${esc(x.praca)}</div>`;
      lp = x.praca;
    }
    body +=
      `<h3>${esc(x.nome)}${x.st === "definir" ? '<span class="tag">a definir</span>' : ""} <small>· produzir ${esc(qU(x.total, x.uu))} · ficha rende ${esc(x.rendLabel)}${x.rend !== 1 ? ` · ${receitas(x.lotes)} a receita` : ""}${x.resp ? ` · resp. ${esc(x.resp)}` : ""}</small></h3>` +
      (x.ins.length
        ? '<table><thead><tr><th class="c" style="width:11mm">Separado</th><th>Insumo</th><th class="r" style="width:34mm">Por receita</th><th class="r" style="width:34mm">Total do evento</th></tr></thead><tbody>' +
          x.ins.map((i) => `<tr><td class="c">${pBox}</td><td>${esc(i.n)}${i.via ? ` <span style="color:#7a776f;font-size:8pt">· via ${esc(i.via)}</span>` : ""}</td><td class="r">${esc(qU(i.qb, i.u))}</td><td class="r big">${esc(qU(i.qt, i.u))}</td></tr>`).join("") +
          "</tbody></table>"
        : '<p class="note">Ficha sem insumos cadastrados.</p>') +
      (x.modo ? `<p class="note" style="white-space:pre-wrap">${esc(x.modo)}</p>` : "");
  }
  if (!ps.length) body += '<p class="empty">Nenhum prato no cardápio.</p>';
  return czDoc({ titulo: "Insumos por prato", brand: czBrand(pj), sub: "Ingredientes de cada ficha técnica multiplicados pela quantidade do evento", badge: badgePara(pj), body });
}

function printCompras(dados: CozinhaResponseDTO, pj: CozinhaProjecaoDTO, f: FiltroCompra): string {
  const ls = linhasCompra(pj, exDe(dados, `cp_${pj.eventoId}`).itens ?? {}, f);
  let body = "";
  for (const g of CATEGORIAS_INSUMO) {
    const l = ls.filter((x) => ((CATEGORIAS_INSUMO as readonly string[]).includes(x.cat) ? x.cat : "Outros") === g).sort((a, b) => a.n.localeCompare(b.n, "pt-BR"));
    if (!l.length) continue;
    body +=
      `<h2>${esc(g)}<small>${pl(l.length, "item", "itens")}</small></h2><table><thead><tr><th class="c" style="width:13mm">Comprado</th><th>Insumo</th><th class="r" style="width:22mm">Confirmados</th><th class="r" style="width:20mm">A definir</th><th class="r" style="width:22mm">Em estoque</th><th class="r" style="width:24mm">Comprar</th><th style="width:40mm">Usado em</th></tr></thead><tbody>` +
      l
        .map((x) => {
          const emb = embalagens(x);
          return `<tr><td class="c">${x.ok ? pTick : pBox}</td><td><b>${esc(x.n)}</b></td><td class="r">${x.q ? esc(qU(x.q, x.u)) : "—"}</td><td class="r">${x.qd ? esc(qU(x.qd, x.u)) : "—"}</td><td class="r">${esc(qU(x.est, x.u))}</td><td class="r big">${x.falta > 0 ? esc(qU(x.falta, x.u)) : "—"}${emb ? `<br><span style="font-weight:400;color:#7a776f;font-size:8pt">${esc(emb)}</span>` : ""}</td><td class="pd">${esc(x.usos)}</td></tr>`;
        })
        .join("") +
      "</tbody></table>";
  }
  if (!body) body = '<p class="empty">Nada a comprar neste filtro.</p>';
  return czDoc({ titulo: "Lista de compras", brand: czBrand(pj), sub: `Comprar = confirmados − em estoque (físico menos o reservado para outros eventos)${f !== "todos" ? ` · filtro: ${f === "comprar" ? "a comprar" : "comprados"}` : ""}`, badge: badgePara(pj), body });
}

const faixa = (cons: string) => (cons === "refrigerado" ? "0–4 °C" : cons === "congelado" ? "≤ −18 °C" : cons === "quente" ? "≥ 60 °C" : consLabel(cons));

function printChecklist(dados: CozinhaResponseDTO, pj: CozinhaProjecaoDTO): string {
  const rows = czRows(pj);
  const v = (n: unknown) => (n == null || n === "" ? "" : esc(fmtN(Number(n))));
  let body = czObs(pj);
  body +=
    '<p class="note" style="margin-top:0">Conferir na saída do buffet e novamente no retorno. Toda linha precisa de duas assinaturas: quem executou o preparo e quem separou para o transporte. Linhas com ↳ são os componentes que viajam separados — cada uma é conferida individualmente.</p>';
  body +=
    '<table class="ck"><thead><tr><th>Item</th><th class="c" style="width:12mm">Unid.</th><th class="r" style="width:17mm">Qtd prevista</th><th class="r" style="width:17mm">Qtd produzida</th><th style="width:26mm">Executado por</th><th class="r" style="width:19mm">Qtd enviada ao evento</th><th style="width:26mm">Separado por</th><th class="r" style="width:19mm">Qtd retorno ao buffet</th><th style="width:26mm">Conferido no retorno por</th><th style="width:38mm">Observação</th></tr></thead><tbody>';
  for (const sc of CZ_SECS) {
    const l = rows.filter((r) => r.sec === sc[0]);
    if (!l.length) continue;
    body += `<tr class="sech"><td colspan="10">${esc(sc[1].toUpperCase())}${sc[2] ? ` <small>(${esc(sc[2])})</small>` : ""}</td></tr>`;
    for (const r of l) {
      const x = exDe(dados, r.id);
      const ob = [x.obs || "", !x.obs && r.obsP ? r.obsP : "", czFria(r) && r.cons === "refrigerado" ? "0–4 °C" : czFria(r) ? consLabel(r.cons) : ""].filter(Boolean).join(" · ");
      const na = '<span class="na">—</span>';
      body +=
        `<tr class="${r.tipo === "prato" ? "pt" : r.tipo === "comp" ? "cp" : ""}"><td>${r.tipo === "comp" ? `<span class="ar">↳</span> ${esc(r.nome)}` : r.tipo === "prato" ? `<b>${esc(r.nome)}</b>` : esc(r.nome)}</td><td class="c">${esc(r.u || "")}</td><td class="r big">${esc(fmtQ(r.prev, r.uf || r.u))}</td>` +
        `<td class="r">${r.prod ? v(x.produzido) : na}</td><td>${r.prod ? esc(x.executadoPor || "") : na}</td><td class="r">${v(czEnvV(r, x))}</td><td>${esc(x.separadoPor || "")}</td><td class="r">${v(x[r.retK])}</td><td>${esc(x.conferidoPor || "")}</td><td class="ob">${esc(ob)}</td></tr>`;
    }
  }
  if (!rows.length) body += '<tr><td colspan="10" class="empty">Nenhum prato confirmado.</td></tr>';
  body += "</tbody></table>";
  const fr = rows.filter(czFria);
  if (fr.length) {
    body +=
      '<h2>Cadeia fria — registro de temperatura<small>itens já listados acima, não somar de novo</small></h2><table class="ck"><thead><tr><th>Item</th><th class="c" style="width:12mm">Unid.</th><th class="r" style="width:20mm">Qtd prevista</th><th style="width:34mm">Faixa</th><th class="r" style="width:26mm">Temp. saída (°C)</th><th class="r" style="width:26mm">Temp. chegada (°C)</th><th style="width:44mm">Conferido por</th></tr></thead><tbody>' +
      fr
        .map((r) => {
          const x = exDe(dados, r.id);
          return `<tr><td>${esc(r.nome)}${r.prato ? ` <span class="na">— ${esc(r.prato)}</span>` : ""}</td><td class="c">${esc(r.u || "")}</td><td class="r">${esc(fmtQ(r.prev, r.uf || r.u))}</td><td>${esc(faixa(r.cons))}</td><td class="r">${v(x.tempSaida)}</td><td class="r">${v(x.tempChegada)}</td><td>${esc(x.separadoPor || "")}</td></tr>`;
        })
        .join("") +
      "</tbody></table>";
  }
  const tr: Partial<ExecucaoResponseDTO> = exDe(dados, `tr_${pj.eventoId}`);
  const sg = (k: keyof ExecucaoResponseDTO, l: string, k2: keyof ExecucaoResponseDTO, l2: string) => `<div><b>${esc(tr[k] || "")}</b>${l}</div><div><b>${esc(tr[k2] || "")}</b>${l2}</div>`;
  body +=
    `<h2>Assinaturas</h2><div class="sig sig2">${sg("chefPor", "Chef responsável pela produção", "chefHora", "Data e hora")}${sg("cargaPor", "Responsável pela separação e carga", "cargaHora", "Data e hora da saída")}` +
    `${sg("recebPor", "Responsável pelo recebimento no evento", "recebHora", "Data e hora da chegada")}${sg("retornoPor", "Responsável pela conferência do retorno", "retornoHora", "Data e hora do retorno")}</div>`;
  if (tr.veiculo) body += `<p class="note">Veículo: ${esc(tr.veiculo)}</p>`;
  body += `<p class="regras"><b>Regras de conferência:</b> ${CZ_REGRAS.map((t, i) => `(${i + 1}) ${esc(t.charAt(0).toLowerCase() + t.slice(1).replace(/\.$/, ""))}`).join("; ")}.</p>`;
  body +=
    "<style>table.ck td{padding:4px 6px;font-size:8.5pt}table.ck th{font-size:7pt}tr.sech td{background:#2c4a3f;color:#fff;font-weight:700;font-size:8pt;letter-spacing:.06em;padding:5px 7px}tr.sech small{font-weight:400;opacity:.85;letter-spacing:0}tr.pt td{background:#f4f1ea}tr.cp td:first-child{padding-left:16px}.ar{color:#916b2a;font-weight:700}.na{color:#9a968e}td.ob{font-size:7.5pt;color:#55524c}.sig2{grid-template-columns:2fr 1fr 2fr 1fr}.regras{font-size:7.8pt;color:#44423d;margin-top:12px;line-height:1.45}</style>";
  return czDoc({ titulo: "Checklist de produto pronto — saída e retorno", brand: czBrand(pj), sub: `${pj.local ? `${pj.local} · ` : ""}${pj.base} convidados + ${fmtN(pj.margem)}% de margem`, badge: badgePara(pj), body, landscape: true });
}

/** Ordens de produção, uma por cartão, com campos para preencher à mão (protótipo: opPrintHtml). */
function printOrdens(dados: CozinhaResponseDTO, list: OrdemProducaoDTO[], titulo: string, pj: CozinhaProjecaoDTO | null): string {
  const setor = (id: string) => dados.setores?.find((s) => s.id === id)?.nome ?? SETORES_PADRAO.find((s) => s[0] === id)?.[1] ?? id;
  const body = list
    .map((o) => {
      const x = o.exec;
      return (
        '<div style="break-inside:avoid;border:1px solid #e6dcc8;border-radius:6px;padding:10px 12px;margin:0 0 10px">' +
        `<h3 style="margin:0 0 4px">${esc(o.nome)} <small>${esc(`${rotulo(ETAPAS_ORDEM, o.etapa)} · ${setor(o.setor)}`)}</small></h3>` +
        `<table><tbody><tr><td><b>Evento</b><br>${esc(`${o.codigo} · ${o.eventoNome}`)}</td><td><b>Quantidade</b><br>${esc(o.q ? qU(o.q, o.u) : "—")}</td><td><b>Receitas</b><br>${o.receitas ? esc(`${fmtN(o.receitas)} × ${fmtN(o.rend)} ${o.rendU}`) : "—"}</td><td><b>Prazo</b><br>${esc(`${dnLabel(o.dn)}${o.data ? ` · ${fdate(o.data)}` : ""}${o.hora ? ` até ${o.hora}` : ""}`)}</td></tr>` +
        `<tr><td><b>Responsável</b><br>${esc(o.resp || "")}&nbsp;</td><td><b>Tempo estimado</b><br>${o.tempoMin ? `${o.tempoMin} min` : "—"}</td><td><b>Equipamentos</b><br>${esc(o.equip || "—")}</td><td><b>Armazenamento</b><br>${esc(o.armaz || "—")}${o.valDias ? ` · validade ${o.valDias} d` : ""}</td></tr></tbody></table>` +
        (o.ins.length
          ? `<table><thead><tr><th>Ingrediente</th><th class="r">Qtd</th><th class="c" style="width:12mm">Ok</th></tr></thead><tbody>${o.ins.map((i) => `<tr><td>${i.sub ? `↳ ${esc(i.n)} (sub-receita)` : esc(i.n)}</td><td class="r">${esc(qU(i.q, i.u))}</td><td class="c">${pBox}</td></tr>`).join("")}</tbody></table>`
          : "") +
        (o.passos.length ? `<p style="margin:6px 0 2px"><b>Como executar</b></p><ol style="margin:0;padding-left:18px">${o.passos.map((p) => `<li>${esc(p)}</li>`).join("")}</ol>` : "") +
        `<div class="sig"><div><b>${esc(x.iniciadoPor)}</b>Iniciou · hora</div><div><b>${esc(x.qtd != null ? qU(x.qtd, o.u) : "")}</b>Qtd produzida · perda</div><div><b>${esc(x.conferidoPor)}</b>Conferiu · lote</div></div>` +
        '<p class="note">Observações: ______________________________________________</p></div>'
      );
    })
    .join("");
  return czDoc({ titulo, brand: pj ? czBrand(pj) : "Anne Buffet · Produção", sub: pl(list.length, "ordem", "ordens"), badge: pj ? ["Evento", pj.codigo] : undefined, body: (pj ? czObs(pj) : "") + (body || '<p class="empty">Nenhuma ordem.</p>') });
}

/** HTML do documento A4 da aba aberta. */
export function documentoCozinha(dados: CozinhaResponseDTO, o: OpcoesImpressao): string {
  const pj = o.pj;
  const ordens = dados.ordens ?? [];
  if (o.aba === "lider" || o.aba === "indicadores") return printOrdens(dados, ordens.filter((x) => x.data === hoje() || x.atrasada), "Produções de hoje e atrasadas", null);
  if (o.aba === "ordens" && pj) return printOrdens(dados, ordens.filter((x) => x.eventoId === pj.eventoId && (!o.setor || x.setor === o.setor)), `Ordens de produção${o.setor ? ` · ${o.setor}` : ""}`, pj);
  if (o.aba === "agenda" || !pj) return printAgenda(dados, o.semana, o.praca);
  if (o.aba === "producao") return printProducao(dados, pj);
  if (o.aba === "insumos") return printInsumos(pj);
  if (o.aba === "compras") return printCompras(dados, pj, o.filtroCompra);
  if (o.aba === "checklist") return printChecklist(dados, pj);
  return printCrono(dados, pj, o.praca);
}

export function nomeArquivoCozinha(o: OpcoesImpressao): string {
  return `cozinha-${o.aba}${o.aba !== "agenda" && o.pj ? `-${o.pj.codigo}` : ""}-${hoje()}.html`.replace(/[^a-z0-9.\-]/gi, "-");
}

/**
 * Abre o documento em outra janela (ele mesmo chama a impressão). Se o
 * navegador bloquear a janela, baixa o arquivo para abrir e imprimir.
 * Devolve como saiu, para a tela avisar.
 */
export function imprimirCozinha(dados: CozinhaResponseDTO, o: OpcoesImpressao): "janela" | "arquivo" {
  return abrirDocumento(documentoCozinha(dados, o), nomeArquivoCozinha(o));
}
