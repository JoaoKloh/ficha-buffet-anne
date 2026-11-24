/** Ícones de linha do protótipo (menu e KPIs), em SVG inline. */
const P = { viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: 1.8, strokeLinecap: "round" as const, strokeLinejoin: "round" as const, "aria-hidden": true };

export const ICONES: Record<string, React.ReactElement> = {
  editar: (<svg {...P}><path d="M4 20h4L18.5 9.5a2.1 2.1 0 0 0-4-4L4 16v4z" /><path d="M13.5 6.5l4 4" /></svg>),
  painel: (<svg {...P}><rect x="3" y="3" width="7.5" height="9" rx="2" /><rect x="13.5" y="3" width="7.5" height="5" rx="2" /><rect x="13.5" y="11" width="7.5" height="10" rx="2" /><rect x="3" y="15" width="7.5" height="6" rx="2" /></svg>),
  eventos: (<svg {...P}><rect x="3" y="4.5" width="18" height="16" rx="2.5" /><path d="M3 9.5h18M8 2.5v4M16 2.5v4" /></svg>),
  financeiro: (<svg {...P}><path d="M4 7h16M4 12h16M4 17h10" /><path d="M18 15v5M15.5 17.5h5" /></svg>),
  dre: (<svg {...P}><path d="M4 20V10M10 20V4M16 20v-7M22 20H2" /></svg>),
  clientes: (<svg {...P}><circle cx="9" cy="8" r="3.5" /><path d="M2.5 20c.8-3.6 3.3-5.5 6.5-5.5s5.7 1.9 6.5 5.5" /><path d="M16 4.8a3.5 3.5 0 0 1 0 6.4M18.5 14.8c1.6.7 2.6 2.4 3 5.2" /></svg>),
  fichas: (<svg {...P}><path d="M6 3h9l4 4v14H6z" /><path d="M15 3v4h4M9 12h7M9 16h7M9 8h3" /></svg>),
  cozinha: (<svg {...P}><path d="M6 13.5V20h12v-6.5" /><path d="M6 13.5a4 4 0 0 1-.9-7.9A5 5 0 0 1 12 3a5 5 0 0 1 6.9 2.6 4 4 0 0 1-.9 7.9z" /><path d="M6 17h12" /></svg>),
  producao: (<svg {...P}><path d="M3 21h18M5 21V10l5 3V10l5 3V6l4 2v13" /><path d="M9 17h2M14 17h2" /></svg>),
  estoque: (<svg {...P}><path d="M3 8l9-5 9 5v8l-9 5-9-5z" /><path d="M3 8l9 5 9-5M12 13v8" /></svg>),
  money: (<svg {...P}><path d="M3 17l6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>),
  wallet: (<svg {...P}><rect x="3" y="6" width="18" height="14" rx="2.5" /><path d="M3 10h18M16 15h2" /></svg>),
  clock: (<svg {...P}><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></svg>),
  chart: (<svg {...P}><path d="M4 20V11M10 20V5M16 20v-6M22 20H2" /></svg>),
  down: (<svg {...P}><path d="M3 7l6 6 4-4 8 8" /><path d="M15 17h6v-6" /></svg>),
  list: (<svg {...P}><path d="M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01" /></svg>),
  box: (<svg {...P}><path d="M3 8l9-5 9 5v8l-9 5-9-5z" /><path d="M3 8l9 5 9-5M12 13v8" /></svg>),
  cal: (<svg {...P}><rect x="3" y="4.5" width="18" height="16" rx="2.5" /><path d="M3 9.5h18M8 2.5v4M16 2.5v4" /></svg>),
};

/** Ícone do KPI pelo texto do rótulo, como no protótipo. */
export function iconeKpi(rotulo: string): React.ReactElement {
  const t = rotulo.toLowerCase();
  if (/a receber|a pagar|vence|prazo/.test(t)) return ICONES.clock!;
  if (/recebid|pago|caixa|saldo/.test(t)) return ICONES.wallet!;
  if (/resultado|margem|lucro|cmv/.test(t)) return ICONES.chart!;
  if (/despes|custo|perda|sa[ií]da/.test(t)) return ICONES.down!;
  if (/fatur|receit|venda|ticket/.test(t)) return ICONES.money!;
  if (/estoque|ite|insumo|valor|lote/.test(t)) return ICONES.box!;
  if (/evento|data|dia|prato/.test(t)) return ICONES.cal!;
  return ICONES.list!;
}
