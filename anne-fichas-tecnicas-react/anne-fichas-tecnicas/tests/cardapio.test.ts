import { describe, expect, it } from "vitest";
import { filtrarPratos, type GrupoPratos } from "@/lib/utils/cardapio";

const preps: GrupoPratos[] = [
  { grupo: "Entradas", itens: [[1, "Carpaccio de carne"], [2, "Quiche de alho-poró"], [3, "Pão de queijo"]] },
  { grupo: "Principais", itens: [[4, "Risoto de brie e aspargos"], [5, "Filé ao molho madeira"], [6, "Galinhada"]] },
  { grupo: "Sobremesas", itens: [[7, "Pavê de chocolate"], [8, "Crème brûlée"]] },
];
const ids = (gs: GrupoPratos[]) => gs.flatMap((g) => g.itens.map(([id]) => id));

describe("filtro do Adicionar pratos ao cardápio", () => {
  it("sem busca e sem categoria devolve todos", () => {
    expect(ids(filtrarPratos(preps, "", ""))).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    expect(ids(filtrarPratos(preps, "", "   "))).toHaveLength(8);
  });
  it("nome completo e parcial", () => {
    expect(ids(filtrarPratos(preps, "", "Galinhada"))).toEqual([6]);
    expect(ids(filtrarPratos(preps, "", "de"))).toEqual([1, 2, 3, 4, 5, 7]);
  });
  it("ignora maiúsculas e acentos, nos dois sentidos", () => {
    expect(ids(filtrarPratos(preps, "", "PAO"))).toEqual([3]);
    expect(ids(filtrarPratos(preps, "", "pão"))).toEqual([3]);
    expect(ids(filtrarPratos(preps, "", "creme brulee"))).toEqual([8]);
    expect(ids(filtrarPratos(preps, "", "PAVÉ"))).toEqual([7]);
    expect(ids(filtrarPratos(preps, "", "file"))).toEqual([5]);
  });
  it("combina com a categoria e some com grupos vazios", () => {
    expect(filtrarPratos(preps, "Entradas", "de").map((g) => g.grupo)).toEqual(["Entradas"]);
    expect(ids(filtrarPratos(preps, "Sobremesas", "de"))).toEqual([7]);
    expect(filtrarPratos(preps, "Principais", "chocolate")).toEqual([]);
    expect(filtrarPratos(preps, "", "lasanha")).toEqual([]);
  });
  it("limpar a busca volta tudo da categoria", () => {
    expect(ids(filtrarPratos(preps, "Principais", ""))).toEqual([4, 5, 6]);
  });
  it("não altera os dados de entrada", () => {
    const antes = JSON.stringify(preps);
    filtrarPratos(preps, "Entradas", "pao");
    expect(JSON.stringify(preps)).toBe(antes);
  });
});
