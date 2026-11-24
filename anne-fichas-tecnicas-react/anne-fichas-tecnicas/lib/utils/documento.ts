/**
 * Documento A4 dos relatórios (modelo "Cronograma da semana" do protótipo): cabeçalho com marca,
 * título, subtítulo e selo, corpo em HTML e rodapé com data e página. Usado pela Cozinha, pela DRE
 * e pela Produção; abre em outra janela para imprimir ou salvar em PDF pelo navegador.
 */

export const esc = (s: unknown): string =>
  String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);

export interface DocA4 {
  titulo: string;
  brand: string;
  sub?: string;
  badge?: [string, string];
  /** HTML já escapado. */
  body: string;
  landscape?: boolean;
}

const p2 = (n: number) => String(n).padStart(2, "0");

export function docA4Html(o: DocA4): string {
  const now = new Date();
  const ger = `Gerado em ${p2(now.getDate())}/${p2(now.getMonth() + 1)}, ${p2(now.getHours())}:${p2(now.getMinutes())}`;
  return (
    `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${esc(`${o.titulo} · ${o.brand}`)}</title><style>` +
    `@page{size:A4 ${o.landscape ? "landscape" : "portrait"};margin:13mm 12mm 15mm;@bottom-left{content:"${ger}";font:7.5pt Helvetica,Arial,sans-serif;color:#8a8076}@bottom-right{content:"Página " counter(page) " de " counter(pages);font:7.5pt Helvetica,Arial,sans-serif;color:#8a8076}}` +
    "*{box-sizing:border-box}html{-webkit-print-color-adjust:exact;print-color-adjust:exact}body{margin:0;background:#fff;color:#22211f;font:9.5pt/1.35 Helvetica,Arial,sans-serif}" +
    `.pg{max-width:${o.landscape ? "277mm" : "186mm"};margin:0 auto;padding:8mm 0}@media print{.pg{padding:0;max-width:none}.bar{display:none}}` +
    ".bar{position:sticky;top:0;z-index:2;background:#2c5a4b;color:#fff;padding:10px 16px;font:600 13px Helvetica,Arial,sans-serif;display:flex;gap:12px;align-items:center;justify-content:space-between}.bar button{font:inherit;border:0;border-radius:8px;padding:7px 14px;cursor:pointer;background:#fff;color:#2c5a4b}" +
    ".brand{font-size:7.5pt;letter-spacing:.22em;text-transform:uppercase;color:#916b2a}" +
    ".hd{display:flex;justify-content:space-between;align-items:flex-end;gap:16px;border-bottom:1px solid #e6dcc8;padding-bottom:9px;margin:4px 0 14px}" +
    'h1{font:400 21pt/1.1 Georgia,"Times New Roman",serif;color:#2c4a3f;margin:0}.sub{color:#6b6a66;font-size:9pt;margin:3px 0 0}' +
    ".badge{background:#2c4a3f;color:#fff;border-radius:6px;padding:7px 14px;min-width:130px;flex:none}.badge span{display:block;font-size:6.5pt;letter-spacing:.14em;text-transform:uppercase;opacity:.85}.badge b{font:700 13pt Georgia,serif}" +
    'h2{font:700 12pt Georgia,"Times New Roman",serif;color:#2c4a3f;margin:16px 0 6px;break-after:avoid}h2 small{font:400 7.5pt Helvetica,Arial,sans-serif;color:#8a8076;margin-left:6px}' +
    "h3{font:700 10pt Helvetica,Arial,sans-serif;margin:12px 0 4px;break-after:avoid}h3 small{font-weight:400;color:#6b6a66}" +
    ".grp{font-size:7.5pt;letter-spacing:.16em;text-transform:uppercase;color:#916b2a;font-weight:700;margin:16px 0 4px;break-after:avoid}" +
    "table{width:100%;border-collapse:collapse;margin:0 0 4px}thead{display:table-header-group}tr{break-inside:avoid}" +
    "th{background:#e4ece7;color:#2c4a3f;font-size:7.5pt;font-weight:700;text-align:left;padding:6px 7px;border:1px solid #e6dcc8}" +
    "td{border:1px solid #e6dcc8;padding:6px 7px;vertical-align:middle}td.r,th.r{text-align:right}td.c,th.c{text-align:center}" +
    "td.pr{font-weight:700;color:#2c4a3f;font-size:8pt}td.pd{color:#7a776f;font-size:8pt}td.wr{min-width:28mm}td.big{font-weight:700}" +
    ".bx{display:inline-block;width:13px;height:13px;border:1.4px solid #333;border-radius:2px;vertical-align:middle;text-align:center;font:700 10px/11px Helvetica}" +
    ".tag{display:inline-block;font-size:6.5pt;font-weight:700;letter-spacing:.08em;text-transform:uppercase;border-radius:3px;padding:1px 4px;background:#f8ead3;color:#9a5d0b;margin-left:4px;vertical-align:1px}" +
    ".ev{font-weight:700;font-size:8pt;color:#916b2a;white-space:nowrap}" +
    ".obs{border:1px solid #e6dcc8;background:#faf7f1;border-radius:6px;padding:8px 10px;margin:0 0 12px;font-size:8.5pt;white-space:pre-wrap}.obs b{color:#2c4a3f}" +
    ".warn{color:#9a5d0b;font-size:8.5pt;margin:0 0 10px}.note{color:#8a8076;font-size:8pt;margin:4px 0 10px}" +
    ".sig{display:grid;grid-template-columns:repeat(3,1fr);gap:14px 18px;margin-top:10px}.sig div{border-bottom:1px solid #333;padding-top:22px;font-size:7.5pt;color:#6b6a66}.sig div b{display:block;color:#22211f;font-size:9pt;font-weight:400;margin-bottom:2px;min-height:12px}" +
    ".empty{color:#8a8076;padding:10px 0}" +
    "@media print{.bar{display:none!important}}" +
    `</style></head><body><div class="bar"><span>Buffet Anne · ${esc(o.titulo)}</span><button onclick="window.print()">Imprimir ou salvar PDF</button></div><div class="pg">` +
    `<div class="brand">${esc(o.brand)}</div><div class="hd"><div><h1>${esc(o.titulo)}</h1>${o.sub ? `<p class="sub">${esc(o.sub)}</p>` : ""}</div>${o.badge ? `<div class="badge"><span>${esc(o.badge[0])}</span><b>${esc(o.badge[1])}</b></div>` : ""}</div>` +
    `${o.body}</div><script>setTimeout(function(){try{window.print()}catch(e){}},600)</script></body></html>`
  );
}


/**
 * Abre o documento em outra janela (ele mesmo chama a impressão). Se o navegador bloquear
 * a janela, baixa o arquivo para abrir e imprimir. Devolve como saiu, para a tela avisar.
 */
export function abrirDocumento(html: string, nomeArquivo: string): "janela" | "arquivo" {
  const url = URL.createObjectURL(new Blob([html], { type: "text/html;charset=utf-8" }));
  const w = window.open(url, "_blank");
  if (w) {
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
    return "janela";
  }
  const a = document.createElement("a");
  a.href = url;
  a.download = nomeArquivo;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  return "arquivo";
}

/** Nome de arquivo seguro: sem acentos nem caracteres estranhos. */
export const nomeArquivo = (base: string, ext = "html"): string =>
  `${base
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9.\-]+/g, "-")}.${ext}`;
