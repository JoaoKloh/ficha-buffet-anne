/**
 * Categorias do catálogo de pratos e os padrões de produção por convidado.
 * Fonte da verdade única — usado tanto pelo cliente quanto pelas rotas de API.
 */

export const CATEGORIAS = [
  "Petit Gourmet Volante",
  "Canapés Frios",
  "Canapés Quentes",
  "Forno e Fogão",
  "Mini Jantar Volante",
  "Mini Jantar Estacionado",
  "Ilha Estacionada / Antepasto",
  "Sobremesas",
  "Estações Extras",
] as const;

export type Categoria = (typeof CATEGORIAS)[number];

export function isCategoria(value: string): value is Categoria {
  return (CATEGORIAS as readonly string[]).includes(value);
}

export interface PadraoProducao {
  rendQtd: number;
  rendUnid: string;
  porPessoa: number;
}

/** Padrão de rendimento/sugestão por convidado, por categoria. Editável apenas no servidor. */
export const PADRAO_POR_CATEGORIA: Record<Categoria, PadraoProducao> = {
  "Petit Gourmet Volante": { rendQtd: 20, rendUnid: "unidades", porPessoa: 1 },
  "Canapés Frios": { rendQtd: 30, rendUnid: "unidades", porPessoa: 1.5 },
  "Canapés Quentes": { rendQtd: 30, rendUnid: "unidades", porPessoa: 1.5 },
  "Forno e Fogão": { rendQtd: 50, rendUnid: "unidades", porPessoa: 2 },
  "Mini Jantar Volante": { rendQtd: 1000, rendUnid: "g", porPessoa: 100 },
  "Mini Jantar Estacionado": { rendQtd: 1000, rendUnid: "g", porPessoa: 200 },
  "Ilha Estacionada / Antepasto": { rendQtd: 1000, rendUnid: "g", porPessoa: 50 },
  "Sobremesas": { rendQtd: 20, rendUnid: "unidades", porPessoa: 1 },
  "Estações Extras": { rendQtd: 1000, rendUnid: "g", porPessoa: 50 },
};
