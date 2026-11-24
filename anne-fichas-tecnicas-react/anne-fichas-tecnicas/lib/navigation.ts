/**
 * Estrutura de navegação da aplicação, agrupada por módulo. A sidebar e o
 * título do Header leem daqui — para um novo módulo/página basta adicionar
 * uma entrada, sem tocar nos componentes.
 */

export interface NavItem {
  label: string;
  href: string;
  /** Ícone do menu (chave de components/ui/Icones). */
  icone?: string;
  /**
   * Outras rotas que pertencem a este item (ex.: telas abertas a partir dele
   * que não têm entrada própria no menu). Também valem para sub-rotas.
   */
  match?: string[];
}

export interface NavModule {
  label: string;
  items: NavItem[];
}

export const NAV_MODULES: NavModule[] = [
  { label: "Visão geral", items: [{ label: "Painel", href: "/", icone: "painel", match: ["/backup"] }] },
  {
    label: "Operação",
    items: [
      { label: "Eventos", href: "/eventos", icone: "eventos" },
      { label: "Cozinha", href: "/cozinha", icone: "cozinha" },
      { label: "Produção", href: "/producao", icone: "producao" },
      { label: "Fichas", href: "/fichas", icone: "fichas" },
      { label: "Estoque", href: "/estoque", icone: "estoque" },
    ],
  },
  {
    label: "Financeiro",
    items: [
      { label: "Caixa", href: "/caixa", icone: "financeiro" },
      { label: "DRE", href: "/dre", icone: "dre" },
    ],
  },
  { label: "Relacionamento", items: [{ label: "Clientes", href: "/clientes", icone: "clientes" }] },
  {
    label: "Catálogo e orçamento",
    items: [
      { label: "Catálogo de pratos", href: "/catalogo", icone: "fichas" },
      { label: "Ingredientes do catálogo", href: "/ingredientes", icone: "estoque" },
      { label: "Precificação de eventos", href: "/orcamento", icone: "financeiro" },
    ],
  },
];

/** Tamanho do caminho casado (0 = não casa). "/" só casa com a raiz. */
function matchLength(href: string, pathname: string): number {
  if (href === "/") return pathname === "/" ? 1 : 0;
  return pathname === href || pathname.startsWith(`${href}/`) ? href.length : 0;
}

function itemMatchLength(item: NavItem, pathname: string | null): number {
  if (!pathname) return 0;
  return Math.max(...[item.href, ...(item.match ?? [])].map((h) => matchLength(h, pathname)));
}

/**
 * Item do menu da página atual. Vence o caminho mais específico.
 */
export function findActiveNav(pathname: string | null): { mod: NavModule; item: NavItem } | null {
  let melhor: { mod: NavModule; item: NavItem } | null = null;
  let melhorTam = 0;
  for (const mod of NAV_MODULES) {
    for (const item of mod.items) {
      const tam = itemMatchLength(item, pathname);
      if (tam > melhorTam) {
        melhor = { mod, item };
        melhorTam = tam;
      }
    }
  }
  return melhor;
}
