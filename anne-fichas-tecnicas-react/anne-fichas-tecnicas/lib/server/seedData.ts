import { randomUUID } from "crypto";
import type { Dish } from "@/lib/models/dish";
import { PADRAO_POR_CATEGORIA, type Categoria } from "@/lib/models/category";
import { gerarFicha } from "@/lib/utils/recipeGenerator";
import { SEED_DISHES_BY_CATEGORY } from "./seedCatalog";

/**
 * Monta a carga inicial completa do catálogo (usada apenas quando o
 * repositório de pratos está vazio — ver dishRepository.seedIfEmpty).
 */
export function buildSeedDishes(): Dish[] {
  const list: Dish[] = [];
  const timestamp = new Date().toISOString();

  (Object.entries(SEED_DISHES_BY_CATEGORY) as [Categoria, string[]][]).forEach(
    ([categoria, nomes]) => {
      const padrao = PADRAO_POR_CATEGORIA[categoria];
      nomes.forEach((nome) => {
        const ficha = gerarFicha(nome);
        list.push({
          id: randomUUID(),
          nome,
          categoria,
          foto: "",
          rendQtd: padrao.rendQtd,
          rendUnid: padrao.rendUnid,
          porPessoa: padrao.porPessoa,
          receita: ficha.receita,
          ingredientes: ficha.ingredientes,
          createdAt: timestamp,
          updatedAt: timestamp,
        });
      });
    }
  );

  return list;
}
