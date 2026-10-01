import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";

/**
 * Busca o catálogo real de pratos (com o `id` do banco) para popular o menu
 * suspenso "Selecione um prato" em ProductionFormModal — mesmo padrão de
 * listarIngredientesDisponiveis em lib/api/pratoIngredientes.ts. Precisa vir
 * do backend real, não de um mock local, porque é de lá que sai o `pratoId`
 * que CreateProducaoRequestDTO/UpdateProducaoRequestDTO exigem.
 */
export async function listarPratosDisponiveis(): Promise<PratoDetalhadoResponseDTO[]> {
  const res = await fetch("http://localhost:8080/prato/retornarTodos", {
    credentials: "include",
  });

  if (!res.ok) {
    throw new Error("Não foi possível carregar os pratos cadastrados.");
  }

  return res.json();
}
