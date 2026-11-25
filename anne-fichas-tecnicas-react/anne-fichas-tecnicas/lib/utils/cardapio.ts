import { semAcento } from "@/lib/utils/format";

export type GrupoPratos = { grupo: string; itens: [number, string][] };

/**
 * Filtro do "Adicionar pratos ao cardápio": categoria escolhida ("" = todas) e busca pelo nome,
 * sem diferenciar maiúsculas nem acentos. Grupos que ficam sem pratos saem da lista.
 */
export function filtrarPratos(preparacoes: GrupoPratos[], grupo: string, busca: string): GrupoPratos[] {
  const q = semAcento(busca.trim());
  return preparacoes
    .filter((g) => !grupo || g.grupo === grupo)
    .map((g) => ({ ...g, itens: g.itens.filter(([, n]) => !q || semAcento(n).includes(q)) }))
    .filter((g) => g.itens.length);
}
