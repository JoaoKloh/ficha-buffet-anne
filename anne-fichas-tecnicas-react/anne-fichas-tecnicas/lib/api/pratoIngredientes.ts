import { ingredientsApi } from "./ingredients";
import type { IngredienteResponseDTO } from "@/lib/models/prato";

/**
 * Busca o catálogo real de ingredientes (com o `id` do banco) para popular o
 * menu suspenso em IngredientRows. Vai para /api/ingredients (mesma origem do
 * frontend, proxy do IngredientesController real) — não para o backend Java
 * diretamente — mesmo padrão de dishesApi/pratoProducoes. É de lá que sai o
 * `ingredienteId` que CreatePratoRequestDTO/UpdatePratoRequest exigem.
 */
export async function listarIngredientesDisponiveis(): Promise<IngredienteResponseDTO[]> {
  return ingredientsApi.list();
}
