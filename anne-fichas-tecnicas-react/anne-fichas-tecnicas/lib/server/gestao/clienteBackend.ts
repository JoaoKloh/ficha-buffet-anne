import { java, query } from "./java";
import type { ClienteListaItemResponseDTO, ClienteResponseDTO, CreateClienteRequestDTO, UpdateClienteRequestDTO } from "@/lib/models/cliente";

/** Clientes: ClienteController (/clientes) do backend Java. */
export const clienteBackend = {
  /** Um cliente com o resumo de eventos (ficha do evento abre o cadastro). */
  detalhar: (id: number) => java.get<ClienteListaItemResponseDTO>(`/clientes/detalhar/${id}`),
  listar: (busca = "") => java.get<ClienteListaItemResponseDTO[]>(`/clientes/retornarTodos${query({ busca })}`),
  criar: (dto: CreateClienteRequestDTO) => java.post<ClienteResponseDTO>("/clientes/criar", dto),
  atualizar: ({ id, ...dto }: UpdateClienteRequestDTO) => java.put<ClienteResponseDTO>(`/clientes/atualizar/${id}`, dto),
  apagar: (id: number) => java.delete(`/clientes/apagar/${id}`),
};
