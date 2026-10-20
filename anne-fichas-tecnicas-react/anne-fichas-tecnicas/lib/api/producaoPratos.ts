import { apiClient } from "./client";
import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";

/**
 * Busca o catálogo real de pratos (com o `id` do banco) para popular o menu
 * suspenso "Selecione um prato" em ProductionFormModal. Vai para /api/prato
 * (mesma origem do frontend, proxy do PratoController real) — não para o
 * backend Java diretamente — mesmo padrão de listarProducoesDisponiveis em
 * lib/api/pratoProducoes.ts. É de lá que sai o `pratoId` que
 * CreateProducaoRequestDTO/UpdateProducaoRequestDTO exigem.
 */
export async function listarPratosDisponiveis(): Promise<PratoDetalhadoResponseDTO[]> {
  return apiClient.get<PratoDetalhadoResponseDTO[]>("/api/prato");
}
