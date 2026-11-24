import type { Conservacao } from "@/lib/models/dominio";
import type { CozinhaProjecaoDTO, ExecucaoResponseDTO } from "@/lib/models/cozinha";
import { r3 } from "./datas";
import { fmtN } from "./numero";

/**
 * Regras puras do checklist de envio da cozinha. O BFF usa para montar a
 * projeção e validar o que é gravado; a tela usa para mostrar a situação de
 * cada linha enquanto a chef digita (mesma função, sem cópia).
 */

/** Arredonda quantidade do checklist: kg/L a 0,1; o resto para cima. */
export function czArred(t: number, u: string): number {
  if (!(t > 0)) return 0;
  if (u === "kg" || u === "L" || u === "l") return Math.max(0.1, Math.round(t * 10) / 10);
  return Math.ceil(t - 1e-9);
}
export function czExtraQ(x: { modo: string; v: number; u: string }, nd: { base: number; N: number }): number {
  const v = Number(x.v) || 0;
  if (x.modo === "fix") return r3(v);
  if (x.modo === "pax106") return r3(Math.ceil((nd.base || 0) * 1.06 - 1e-9) * v);
  return r3(czArred(v * (nd.N || 0), x.u || "un"));
}

export const CZ_SECS = [
  ["ilha", "Ilha de antepasto", "viaja em componentes, montar só no local"],
  ["canape", "Finger food", "canapés frios e quentes"],
  ["volante", "Forno e fogão volantes", ""],
  ["entrada", "Petit gourmet e mini porções", ""],
  ["jantar", "Jantar", ""],
  ["sobremesa", "Sobremesa", ""],
  ["bebida", "Bebidas do cardápio", ""],
  ["outros", "Outros", ""],
  ["servico", "Bebidas e insumos do serviço", "confirmar o que é fornecido pelo buffet"],
  ["loucas", "Prataria, louças e utensílios", "conferir tudo no retorno"],
  ["descartaveis", "Descartáveis e decor", ""],
] as const;
export const czSecDe = (cat: string): string => (cat === "principal" || cat === "acompanhamento" ? "jantar" : CZ_SECS.some((x) => x[0] === cat) ? cat : "outros");

/** Lista padrão do modelo "Checklist de produto pronto": [sec, nome, unidade, modo, valor, obs]. */
export const CZ_PADRAO: readonly (readonly [string, string, string, string, number, string?])[] = [
  ["servico", "Água sem gás", "L", "pax", 0.35], ["servico", "Água com gás", "L", "pax", 0.15], ["servico", "Coca-Cola Zero (lata 350 ml)", "un", "pax", 0.63],
  ["servico", "Guaraná (lata 350 ml)", "un", "pax", 0.38], ["servico", "Limão taiti", "kg", "pax", 0.008],
  ["servico", "Óleo de fritura (volante)", "L", "pax", 0.025, "Pastel, coxinha, bolinhas, croquete, dadinho"], ["servico", "Gelo", "kg", "pax", 1],
  ["loucas", "Bandeja vintage de prata", "un", "fix", 20, "Volante + reserva — ajustar ao nº de garçons"], ["loucas", "Travessa alta de prata", "un", "fix", 2, "Brie folhado"],
  ["loucas", "Travessa rasa de prata", "un", "fix", 4, "Gravlax 2 · burrata 2"], ["loucas", "Cuba de prata sobre gelo", "un", "fix", 2, "Ceviche"],
  ["loucas", "Baixela alta de prata", "un", "fix", 2, "Torre de figo"], ["loucas", "Prato raso de prata", "un", "fix", 4, "Carpaccio"],
  ["loucas", "Réchaud pequeno", "un", "fix", 2, "Cogumelos flambados"], ["loucas", "Bowl de prata", "un", "fix", 2, "Guacamole"],
  ["loucas", "Cesta para nachos e pães", "un", "fix", 4], ["loucas", "Tábua de madeira para favo", "un", "fix", 2],
  ["loucas", "Colher de servir", "un", "fix", 8, "Burrata 2 · ceviche 2 · cogumelos 2 · guacamole 2"], ["loucas", "Pinça de servir", "un", "fix", 6, "Carpaccio 2 · gravlax 2 · nachos e pães 2"],
  ["loucas", "Réchaud com cuba GN", "un", "fix", 6, "2 por mini porção"], ["loucas", "Fritadeira de apoio", "un", "fix", 2, "Volantes fritos, camarão e churros"],
  ["loucas", "Cesto de bambu para vapor", "un", "fix", 4, "Bao"], ["loucas", "Molheira individual para dip", "un", "fix", 48, "Coxinha, bolinha de bobó, bolinha de queijo, dadinho (12 cada)"],
  ["loucas", "Mini prato de degustação", "un", "pax106", 1, "Polvo"], ["loucas", "Verrine de vidro", "un", "pax106", 1, "Camarão crocante"],
  ["loucas", "Mini cocotte", "un", "pax106", 3, "Arroz thai, mignon com risoto, risoto de cogumelos"], ["loucas", "Colher / garfinho mini", "un", "pax106", 5, "Polvo, camarão e as 3 mini porções"],
  ["loucas", "Bisnaga para doce de leite e chocolate", "un", "fix", 4, "Churros"], ["loucas", "Jarra de vidro", "un", "fix", 24], ["loucas", "Balde de gelo", "un", "fix", 8],
  ["loucas", "Bandeja de recolhimento", "un", "fix", 8], ["loucas", "Lixeira de apoio na cozinha", "un", "fix", 4],
  ["descartaveis", "Palito decorativo", "un", "pax106", 1, "Torre de figo"], ["descartaveis", "Espeto de bambu", "un", "pax106", 1, "Stick de frango"],
  ["descartaveis", "Papel manteiga para vapor", "un", "pax106", 1, "Bao"], ["descartaveis", "Cone de papel para churros", "un", "pax106", 1, "2 churros por cone"],
  ["descartaveis", "Saco de confeitar descartável", "un", "fix", 20, "Sour cream, recheios de macaron, éclair, vol-au-vent"], ["descartaveis", "Guardanapo de papel para volante", "un", "fix", 800],
  ["descartaveis", "Renda para forrar bandeja", "un", "fix", 4], ["descartaveis", "Flores para decor de bandeja", "un", "fix", 8], ["descartaveis", "Luva branca para garçom", "un", "fix", 30],
  ["descartaveis", "Filme PVC e etiquetas de validade", "un", "fix", 2, "Rolos — identificar cada componente"],
];

export const CZ_REGRAS = [
  "Nenhum item sai do buffet sem as duas assinaturas preenchidas (quem executou e quem separou).",
  "A ilha de antepasto viaja em componentes separados — montar só no local; o ceviche é temperado só no local.",
  "Pescados, carnes cruas e itens marcados 0–4 °C são transportados refrigerados, com temperatura registrada na saída e na chegada no bloco de cadeia fria.",
  "Produto perecível montado que retorna não é reaproveitado — registrar na observação e descartar.",
  "Congelados não fritos (coxinha, bolinhas, croquete, pastel, dadinho, churros, camarão empanado) que retornam voltam para a câmara, desde que não tenham descongelado.",
  "Componentes em kg/L seguem o peso dos insumos (arroz em peso cru) — registrar o rendimento real em Qtd produzida.",
  "Cascas e bases crocantes (tuile, macaron, sablé, éclair, vol-au-vent, crosta de parmesão) viajam em caixa fechada, separadas dos recheios.",
  "Prataria e louças: a Qtd retorno tem de bater com a Qtd enviada — qualquer diferença vai para a observação com o nome de quem recebeu.",
];

/** Temperatura fora da faixa da conservação; null = não informada. */
export function tempFora(cons: Conservacao | string | undefined, t: number | null | undefined): boolean | null {
  if (t == null || !isFinite(Number(t))) return null;
  t = Number(t);
  if (cons === "refrigerado") return t > 4 || t < -1;
  if (cons === "congelado") return t > -18;
  if (cons === "quente") return t < 60;
  return false;
}

/* ---------------------------------- Checklist ---------------------------------- */

export interface CkRow {
  tipo: "prato" | "comp" | "extra" | "louca";
  sec: string;
  id: string;
  base: { kind: ExecucaoResponseDTO["kind"]; eventoId: number; cardapioId?: number; key?: string; insumoId?: number };
  nome: string;
  u: string;
  uf?: string;
  prev: number;
  cons: Conservacao | string;
  prod: boolean;
  envK: "enviadoQ" | "saiu";
  retK: "retorno" | "voltou";
  obsP: string;
  prato?: string;
  temComp?: boolean;
  daFicha?: boolean;
  resp?: string;
  insumoId?: number;
}

export function czRows(pj: CozinhaProjecaoDTO): CkRow[] {
  const ev = pj.eventoId;
  const rows: CkRow[] = [];
  const ordSec = (k: string) => CZ_SECS.findIndex((x) => x[0] === k);
  const ps = pj.pratos.filter((x) => x.st === "sim").sort((a, b) => ordSec(czSecDe(a.cat)) - ordSec(czSecDe(b.cat)) || a.nome.localeCompare(b.nome, "pt-BR"));
  for (const p of ps) {
    const sec = czSecDe(p.cat);
    const linha: CkRow = { tipo: "prato", sec, id: `i_${p.cid}`, base: { kind: "item", eventoId: ev, cardapioId: p.cid }, nome: p.nome, u: /porç/.test(p.ulabel || "") ? "porç." : p.uu, uf: p.uu, prev: p.total, cons: p.conserv, prod: true, envK: "enviadoQ", retK: "retorno", temComp: p.comp.length > 0, resp: p.resp, obsP: p.obsEnvio || "" };
    rows.push(linha);
    if (p.comp.length) for (const k of p.comp) rows.push({ tipo: "comp", sec, id: `c_${p.cid}__${k.k}`, base: { kind: "comp", eventoId: ev, cardapioId: p.cid, key: k.k }, nome: k.n, u: k.u, prev: k.prev, cons: k.cons, prod: true, envK: "enviadoQ", retK: "retorno", obsP: k.obs, prato: p.nome });
    else p.ins.forEach((i, ix) => { const k = `f_${i.k || ix}`; rows.push({ tipo: "comp", daFicha: true, sec, id: `c_${p.cid}__${k}`, base: { kind: "comp", eventoId: ev, cardapioId: p.cid, key: k }, nome: i.n + (i.via ? ` (via ${i.via})` : ""), u: i.u, prev: czArred(i.qt, i.u === "l" ? "L" : i.u), cons: i.cons || "ambiente", prod: true, envK: "enviadoQ", retK: "retorno", obsP: "", prato: p.nome }); });
    linha.temComp = rows.some((x) => x.tipo === "comp" && x.prato === p.nome);
  }
  for (const x of pj.extras.filter((x) => x.sec === "servico")) rows.push({ tipo: "extra", sec: "servico", id: `x_${ev}__${x.k}`, base: { kind: "extra", eventoId: ev, key: x.k }, nome: x.n, u: x.u, prev: x.q, cons: "ambiente", prod: false, envK: "enviadoQ", retK: "retorno", obsP: x.obs });
  for (const l of pj.loucas) rows.push({ tipo: "louca", sec: "loucas", id: `l_${ev}__${l.id}`, base: { kind: "louca", eventoId: ev, insumoId: l.id }, nome: l.n, u: "un", prev: l.q, cons: "ambiente", prod: false, envK: "saiu", retK: "voltou", obsP: l.usos, insumoId: l.id });
  for (const x of pj.extras.filter((x) => x.sec !== "servico")) rows.push({ tipo: "extra", sec: x.sec === "descartaveis" ? "descartaveis" : "loucas", id: `x_${ev}__${x.k}`, base: { kind: "extra", eventoId: ev, key: x.k }, nome: x.n, u: x.u, prev: x.q, cons: "ambiente", prod: false, envK: "enviadoQ", retK: "retorno", obsP: x.obs });
  return rows;
}

type Ex = Partial<ExecucaoResponseDTO>;
export const czFria = (r: CkRow): boolean => !!r.cons && r.cons !== "ambiente";
export function czEnvV(r: CkRow, x: Ex): number | null {
  const v = x[r.envK];
  if (v != null) return Number(v);
  if (r.tipo === "prato" && x.enviado) return Number(x.produzido) || null;
  return null;
}
export function czGate(r: CkRow, x: Ex): boolean {
  if (!r.prod) return !!x.separadoPor;
  return (Number(x.produzido) || 0) > 0 && !!x.executadoPor && !!x.separadoPor && (!czFria(r) || (x.tempSaida != null && tempFora(r.cons, x.tempSaida) === false));
}
export function czLoucaSit(x: Ex): [string, string] {
  const sa = x.saiu, vo = x.voltou, av = Number(x.avaria) || 0;
  if (sa == null) return ["", "No depósito"];
  if (vo == null) return ["brass", "Fora no evento"];
  const falta = sa - vo - av;
  if (falta > 0) return ["bad", `Faltam ${fmtN(falta)}`];
  return ["good", av ? "Devolvida (avaria lançada)" : "Devolvida"];
}
export function czRowSt(r: CkRow, x: Ex): [string, string] {
  const tf = czFria(r) && (tempFora(r.cons, x.tempSaida) || tempFora(r.cons, x.tempChegada));
  const env = czEnvV(r, x);
  const ret = x[r.retK];
  if (tf) return ["bad", "Temperatura fora"];
  if (r.tipo === "louca") {
    const s = czLoucaSit(x);
    if (s[0] === "bad" || s[0] === "good") return s;
  } else if (!r.prod && env != null && env > 0 && ret != null && Number(ret) < env) return ["bad", `Faltam ${fmtN(r3(env - Number(ret)))}`];
  if (env != null && env > 0) return ["good", ret != null ? "Retornou" : "Enviado"];
  if (czGate(r, x)) return ["accent", "Pronto para sair"];
  if (r.prod && (Number(x.produzido) || 0) > 0) return ["warn", `Faltam assinaturas${czFria(r) ? " ou temp." : ""}`];
  return ["", "Pendente"];
}
