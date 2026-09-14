import type { Dish } from "@/lib/models/dish";
import type {
  Production,
  ProductionKitchenRow,
  ConsolidatedIngredientRow,
} from "@/lib/models/production";

export function buildKitchenRows(production: Production, dishes: Dish[]): ProductionKitchenRow[] {
  const byId = new Map(dishes.map((d) => [d.id, d]));
  const rows: (ProductionKitchenRow | null)[] = production.itens.map((item) => {
    const dish = byId.get(item.dishId);
    if (!dish) return null;
    const row: ProductionKitchenRow = {
      dishId: dish.id,
      nome: dish.nome,
      categoria: dish.categoria,
      foto: dish.foto,
      quantidade: item.quantidade,
      rendUnid: dish.rendUnid,
      receita: dish.receita,
    };
    return row;
  });
  return rows.filter((row): row is ProductionKitchenRow => row !== null);
}

export function buildShoppingList(
  production: Production,
  dishes: Dish[]
): ConsolidatedIngredientRow[] {
  const byId = new Map(dishes.map((d) => [d.id, d]));
  const consolidated = new Map<string, ConsolidatedIngredientRow>();

  for (const item of production.itens) {
    const dish = byId.get(item.dishId);
    if (!dish) continue;
    const scale = item.quantidade / (dish.rendQtd || 1);
    for (const ing of dish.ingredientes) {
      const key = `${ing.nome.trim().toLowerCase()}|${ing.unidade.trim().toLowerCase()}`;
      const existing = consolidated.get(key);
      const add = ing.qtd * scale;
      if (existing) {
        existing.total += add;
      } else {
        consolidated.set(key, { nome: ing.nome, unidade: ing.unidade, total: add });
      }
    }
  }

  return Array.from(consolidated.values()).sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));
}
