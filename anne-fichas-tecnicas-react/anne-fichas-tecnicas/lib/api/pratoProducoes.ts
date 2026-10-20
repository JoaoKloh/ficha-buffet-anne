import { apiClient } from "./client";
import type { ProducaoResponseDTO } from "@/lib/models/producao";

/**
 * Busca a lista real de eventos/produções (com o `id` do banco) para popular
 * o menu de associação em AssociateProductionModal. Vai para /api/producao
 * (mesma origem do frontend, proxy do ProducaoController real) — não para o
 * backend Java diretamente — mesmo padrão de ingredientsApi/dishesApi. É de
 * lá que sai o `eventosId` que AssociationPratoProducaoRequestDTO exige.
 */
export async function listarProducoesDisponiveis(): Promise<ProducaoResponseDTO[]> {
  return apiClient.get<ProducaoResponseDTO[]>("/api/producao");
}
