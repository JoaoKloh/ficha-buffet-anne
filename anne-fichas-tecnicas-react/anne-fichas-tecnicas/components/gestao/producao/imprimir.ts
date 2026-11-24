import type { PlanoDetalheResponseDTO } from "@/lib/models/planoProducao";
import { fmtN, qtdU } from "@/lib/utils/numero";
import { abrirDocumento, docA4Html, esc, nomeArquivo } from "@/lib/utils/documento";

/** Plano de produção em documento A4 (protótipo: prodImprimir): lista, compras e insumos dos itens a produzir. */

const box = '<span class="bx"></span>';
const tick = '<span class="bx on">✓</span>';
const periodo = (p: { dataInicio: string; dataFim: string }) =>
  p.dataInicio ? `${p.dataInicio.split("-").reverse().join("/")}${p.dataFim && p.dataFim !== p.dataInicio ? ` a ${p.dataFim.split("-").reverse().join("/")}` : ""}` : "sem data";

export function documentoPlano(d: PlanoDetalheResponseDTO): string {
  const pl = d.plano;
  let body =
    `<h2>Lista de produção <small>${d.linhas.length} itens</small></h2><table><thead><tr><th class="c" style="width:11mm">Feito</th><th>Item</th><th>Praça</th><th class="r">Quantidade</th><th class="r">Receitas</th><th>Validade</th></tr></thead><tbody>` +
    d.linhas
      .map(
        (l) =>
          `<tr><td class="c">${l.feito ? tick : box}</td><td><b>${esc(l.nome ?? "Ficha removida")}</b></td><td>${esc(l.praca)}</td><td class="r big">${esc(qtdU(l.qtd, l.unidade))}</td><td class="r">${esc(fmtN(Math.round(l.receitas * 100) / 100))}</td><td>${l.validadeDias != null ? `${l.validadeDias} dia(s)` : "—"}</td></tr>`
      )
      .join("") +
    "</tbody></table>";
  const c = d.compras.itens;
  body += `<h2>Lista de compras <small>itens ainda não concluídos</small></h2>${
    c.length
      ? `<table><thead><tr><th class="c" style="width:11mm">Ok</th><th>Insumo</th><th class="r">Precisa</th><th class="r">Em estoque</th><th class="r">Comprar</th></tr></thead><tbody>${c
          .map((x) => `<tr><td class="c">${box}</td><td>${esc(x.nome)}</td><td class="r">${esc(qtdU(x.precisa, x.unidade))}</td><td class="r">${esc(qtdU(x.estoque, x.unidade))}</td><td class="r big">${x.falta > 0.0005 ? esc(qtdU(x.falta, x.unidade)) : "ok"}</td></tr>`)
          .join("")}</tbody></table>`
      : '<p class="empty">Nada a comprar.</p>'
  }`;
  for (const x of d.insumos.filter((i) => !i.feito && i.qtd > 0)) {
    body += `<h3>${esc(x.nome)} <small>· produzir ${esc(qtdU(x.qtd, x.unidade))}</small></h3>${
      x.ins.length
        ? `<table><thead><tr><th class="c" style="width:11mm">Separado</th><th>Insumo</th><th class="r">Total</th></tr></thead><tbody>${x.ins.map((i) => `<tr><td class="c">${box}</td><td>${esc(i.n)}</td><td class="r big">${esc(qtdU(i.qt, i.u))}</td></tr>`).join("")}</tbody></table>`
        : '<p class="empty">Ficha sem ingredientes.</p>'
    }`;
  }
  return docA4Html({
    titulo: pl.titulo || "Plano de produção",
    brand: pl.unidade === "chocolates" ? "Chocolates Marie & Anne · Produção" : "Restaurante ANNE · Produção",
    sub: `${periodo(pl)}${pl.obs ? ` · ${pl.obs}` : ""}`,
    badge: ["Itens", String(d.linhas.length)],
    body,
  });
}

export const imprimirPlano = (d: PlanoDetalheResponseDTO) => abrirDocumento(documentoPlano(d), nomeArquivo(`producao-${d.plano.unidade}-${d.plano.dataInicio}`));
