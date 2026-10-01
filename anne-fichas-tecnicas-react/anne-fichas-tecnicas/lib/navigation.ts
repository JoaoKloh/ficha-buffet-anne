/**
 * Estrutura de navegação da aplicação, agrupada por módulo. A sidebar e o
 * título do Header leem daqui — para um novo módulo/página basta adicionar
 * uma entrada, sem tocar nos componentes.
 */

export interface NavItem {
  label: string;
  href: string;
}

export interface NavModule {
  label: string;
  items: NavItem[];
}

export const NAV_MODULES: NavModule[] = [
  {
    label: "Produção",
    items: [
      { label: "Catálogo de pratos", href: "/" },
      { label: "Produção por evento", href: "/producao" },
      { label: "Ingredientes", href: "/ingredientes" },
    ],
  },
  {
    label: "Financeiro",
    items: [{ label: "Lançamentos & DRE", href: "/financeiro" }],
  },
  {
    label: "Orçamento",
    items: [{ label: "Precificação de eventos", href: "/orcamento" }],
  },
];

/** "/" só é ativo na raiz; os demais também valem para sub-rotas (ex.: /producao/12). */
export function isNavItemActive(href: string, pathname: string | null): boolean {
  if (!pathname) return false;
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function findActiveNav(
  pathname: string | null
): { mod: NavModule; item: NavItem } | null {
  for (const mod of NAV_MODULES) {
    const item = mod.items.find((i) => isNavItemActive(i.href, pathname));
    if (item) return { mod, item };
  }
  return null;
}
