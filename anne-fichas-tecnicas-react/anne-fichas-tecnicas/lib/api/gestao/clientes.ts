import { apiClient } from "../client";
import type { ClienteListaItemResponseDTO, ClienteResponseDTO, CreateClienteRequestDTO, UpdateClienteRequestDTO } from "@/lib/models/cliente";

/** Clientes, via BFF /api/gestao/clientes. */
export const listarClientes = (busca = "") => apiClient.get<ClienteListaItemResponseDTO[]>(`/api/gestao/clientes${busca ? `?busca=${encodeURIComponent(busca)}` : ""}`);
export const obterCliente = (id: number) => apiClient.get<ClienteListaItemResponseDTO>(`/api/gestao/clientes?id=${id}`);
export const criarCliente = (dto: CreateClienteRequestDTO) => apiClient.post<ClienteResponseDTO>("/api/gestao/clientes", dto);
export const atualizarCliente = (dto: UpdateClienteRequestDTO) => apiClient.put<ClienteResponseDTO>("/api/gestao/clientes", dto);
export const excluirCliente = (id: number) => apiClient.delete<void>(`/api/gestao/clientes?id=${id}`);
