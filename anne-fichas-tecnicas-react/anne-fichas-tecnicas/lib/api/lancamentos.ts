import { apiClient } from "./client";
import type {
  LancamentoResponseDTO,
  CriarLancamentoDTO,
  AtualizarLancamentoDTO,
} from "@/lib/models/financeiro";

/**
 * Lançamentos financeiros. Vai para /api/lancamentos (mesma origem do
 * frontend, proxy do LancamentoController real) — não para o backend Java
 * diretamente — mesmo padrão de /api/producao.
 */
export async function listarLancamentos(): Promise<LancamentoResponseDTO[]> {
  return apiClient.get<LancamentoResponseDTO[]>("/api/lancamentos");
}

export async function criarLancamento(input: CriarLancamentoDTO): Promise<LancamentoResponseDTO> {
  return apiClient.post<LancamentoResponseDTO>("/api/lancamentos", input);
}

export async function atualizarLancamento(input: AtualizarLancamentoDTO): Promise<LancamentoResponseDTO> {
  return apiClient.put<LancamentoResponseDTO>("/api/lancamentos", input);
}

export async function excluirLancamento(id: number): Promise<void> {
  return apiClient.delete<void>(`/api/lancamentos?id=${id}`);
}
