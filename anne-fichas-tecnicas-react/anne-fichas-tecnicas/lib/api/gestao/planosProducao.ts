import { apiClient } from "../client";
import type { UnidadePlano } from "@/lib/models/dominio";
import type {
  AdicionarItensPlanoRequestDTO,
  CalcularCartaRequestDTO,
  PlanoDetalheResponseDTO,
  PlanoProducaoResponseDTO,
  PlanosProducaoResponseDTO,
  SalvarPlanoRequestDTO,
} from "@/lib/models/planoProducao";

/** Planos de produção do restaurante e da chocolates, via BFF /api/gestao/planos-producao. */
const B = "/api/gestao/planos-producao";
export const telaPlanos = (unidade: UnidadePlano) => apiClient.get<PlanosProducaoResponseDTO>(`${B}?unidade=${unidade}`);
export const detalhePlano = (id: number) => apiClient.get<PlanoDetalheResponseDTO>(`${B}?id=${id}`);
export const criarPlano = (dto: SalvarPlanoRequestDTO) => apiClient.post<PlanoProducaoResponseDTO>(B, dto);
export const atualizarPlano = (id: number, dto: SalvarPlanoRequestDTO) => apiClient.put<PlanoProducaoResponseDTO>(`${B}?id=${id}`, dto);
export const excluirPlano = (id: number) => apiClient.delete<void>(`${B}?id=${id}`);

export const adicionarItensPlano = (id: number, dto: AdicionarItensPlanoRequestDTO) => apiClient.post<PlanoProducaoResponseDTO>(`${B}/${id}/itens`, dto);
export const ajustarItemPlano = (id: number, k: string, qtd: number) => apiClient.patch<PlanoProducaoResponseDTO>(`${B}/${id}/itens?k=${encodeURIComponent(k)}`, { qtd });
export const removerItemPlano = (id: number, k: string) => apiClient.delete<void>(`${B}/${id}/itens?k=${encodeURIComponent(k)}`);
/** Conclui o item: cria o lote em Pré-preparos (validade da ficha) e baixa os insumos. */
export const concluirItemPlano = (id: number, k: string) => apiClient.post<{ loteId: number; codigo: string }>(`${B}/${id}/itens/concluir`, { k });
/** Volta o item para a produção e apaga o lote, se ele ainda não foi usado. */
export const desfazerItemPlano = (id: number, k: string) => apiClient.post<{ ok: true }>(`${B}/${id}/itens/desfazer`, { k });
export const calcularPelaCarta = (id: number, dto: CalcularCartaRequestDTO) => apiClient.post<{ fichas: number }>(`${B}/${id}/carta`, dto);
