import type { DreResponseDTO } from "@/lib/models/caixa";
import { brl, pct } from "@/lib/utils/numero";
import { hoje } from "@/lib/utils/datas";
import { abrirDocumento, docA4Html, esc, nomeArquivo } from "@/lib/utils/documento";

/** DRE em documento A4 (protótipo: dreImprimir), a partir do que o BFF já entregou. */

const CSS =
  "<style>td.r,th.r{text-align:right}tr.sum td{font-weight:700;background:#f3eee2}tr.cat td:first-child{padding-left:20px;color:#55534e}tr.final td{font-weight:700;font-size:11pt;border-top:2px solid #22211f;background:#e3ece5}.neg{color:#a8432f}</style>";
const cls = (t: string) => (t === "sum" ? "sum" : t === "final" ? "final" : t === "cat" ? "cat" : "");
const val = (v: number) => `<td class="r${v < 0 ? " neg" : ""}">${brl(v)}</td>`;

export function documentoDre(d: DreResponseDTO): string {
  let body = `${CSS}<h2>Demonstração de resultado<small>${esc(d.descricaoRegime)}</small></h2>`;
  if (d.comparativo) {
    const c = d.comparativo;
    body += `<table><thead><tr><th>Linha</th>${c.colunas.map((x) => `<th class="r">${esc(x.rotulo)}</th>`).join("")}</tr></thead><tbody>`;
    body += c.linhas.map((l) => `<tr class="${cls(l.tipo)}"><td>${esc(l.rotulo)}</td>${l.valores.map(val).join("")}</tr>`).join("");
    body += `<tr><td>Margem líquida</td>${c.margens.map((m) => `<td class="r">${pct(m)}</td>`).join("")}</tr>`;
    if (c.investimentos) body += `<tr class="cat"><td>Investimentos no período (fora do resultado)</td>${c.investimentos.map((v) => `<td class="r">${brl(v)}</td>`).join("")}</tr>`;
    body += "</tbody></table>";
  } else {
    body += '<table><thead><tr><th>Linha</th><th class="r">Valor</th><th class="r">% receita</th></tr></thead><tbody>';
    body += d.linhas.map((l) => `<tr class="${cls(l.tipo)}"><td>${esc(l.rotulo)}</td>${val(l.valor)}<td class="r">${pct(l.pct)}</td></tr>`).join("");
    body += "</tbody></table>";
  }
  if (d.resultadoPorEvento.length) {
    body += '<h2>Resultado por evento</h2><table><thead><tr><th>Evento</th><th>Data</th><th class="r">Receita</th><th class="r">Custos</th><th class="r">Resultado</th><th class="r">Margem</th></tr></thead><tbody>';
    body += d.resultadoPorEvento
      .map((e) => `<tr><td>${esc(e.nome)}</td><td>${esc(e.dataEvento.split("-").reverse().join("/"))}</td><td class="r">${brl(e.rec)}</td><td class="r">${brl(e.desp)}</td>${val(e.res)}<td class="r">${pct(e.mg)}</td></tr>`)
      .join("");
    body += "</tbody></table>";
  }
  return docA4Html({ titulo: `DRE · ${d.titulo}`, brand: "Anne · Buffet e restaurante", sub: d.descricaoRegime.charAt(0).toUpperCase() + d.descricaoRegime.slice(1), badge: ["Resultado líquido", brl(d.resultado)], body });
}

export const imprimirDre = (d: DreResponseDTO) => abrirDocumento(documentoDre(d), nomeArquivo(`dre-${d.titulo}-${hoje()}`));
