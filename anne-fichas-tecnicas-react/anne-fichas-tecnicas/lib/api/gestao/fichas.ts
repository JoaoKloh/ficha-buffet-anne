import { apiClient } from "../client";
import type { CalcularCustoRequestDTO, CreatePreparacaoRequestDTO, CustoPreparacaoDTO, FichasOpcoesResponseDTO, PreparacaoListaItemDTO, PreparacaoResponseDTO, UpdatePreparacaoRequestDTO } from "@/lib/models/preparacao";
import type { CreateInsumoRequestDTO, InsumoListaItemDTO, InsumoResponseDTO, UpdateInsumoRequestDTO } from "@/lib/models/insumo";
import type { CalcularCmvRequestDTO, CmvDTO, CreatePratoVendaRequestDTO, PratoVendaResponseDTO, PratosVendaResponseDTO, UpdatePratoVendaRequestDTO } from "@/lib/models/pratoVenda";

/** Fichas técnicas, itens e pratos do restaurante, via BFF /api/gestao. */
const B = "/api/gestao";
const q = (busca: string) => (busca ? `?busca=${encodeURIComponent(busca)}` : "");
export const opcoesFichas = () => apiClient.get<FichasOpcoesResponseDTO>(`${B}/fichas/opcoes`);
export const listarPreparacoes = (busca = "") => apiClient.get<PreparacaoListaItemDTO[]>(`${B}/preparacoes${q(busca)}`);
export const calcularCustoFicha = (dto: CalcularCustoRequestDTO) => apiClient.post<CustoPreparacaoDTO>(`${B}/preparacoes/custo`, dto);
export const criarPreparacao = (dto: CreatePreparacaoRequestDTO) => apiClient.post<PreparacaoResponseDTO>(`${B}/preparacoes`, dto);
export const atualizarPreparacao = (dto: UpdatePreparacaoRequestDTO) => apiClient.put<PreparacaoResponseDTO>(`${B}/preparacoes`, dto);
export const excluirPreparacao = (id: number) => apiClient.delete<void>(`${B}/preparacoes?id=${id}`);

export const listarInsumos = (busca = "") => apiClient.get<InsumoListaItemDTO[]>(`${B}/insumos${q(busca)}`);
export const situacaoInsumo = (id: number) => apiClient.get<{ emFichas: number; temMovimentos: boolean }>(`${B}/insumos?situacao=${id}`);
export const criarInsumo = (dto: CreateInsumoRequestDTO) => apiClient.post<InsumoResponseDTO>(`${B}/insumos`, dto);
export const atualizarInsumo = (dto: UpdateInsumoRequestDTO) => apiClient.put<InsumoResponseDTO>(`${B}/insumos`, dto);
export const excluirInsumo = (id: number) => apiClient.delete<void>(`${B}/insumos?id=${id}`);

export const telaPratosVenda = (busca = "") => apiClient.get<PratosVendaResponseDTO>(`${B}/pratos-venda${q(busca)}`);
export const calcularCmv = (dto: CalcularCmvRequestDTO) => apiClient.post<CmvDTO>(`${B}/pratos-venda/custo`, dto);
export const criarPratoVenda = (dto: CreatePratoVendaRequestDTO) => apiClient.post<PratoVendaResponseDTO>(`${B}/pratos-venda`, dto);
export const atualizarPratoVenda = (dto: UpdatePratoVendaRequestDTO) => apiClient.put<PratoVendaResponseDTO>(`${B}/pratos-venda`, dto);
export const excluirPratoVenda = (id: number) => apiClient.delete<void>(`${B}/pratos-venda?id=${id}`);
