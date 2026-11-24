import { z } from "zod";
import { ORIGENS_CLIENTE, TIPOS_CLIENTE, chaves, type OrigemCliente, type TipoCliente } from "./dominio";
import { texto } from "./comum";

/** Cliente do buffet (quem contrata o evento). */
export interface ClienteResponseDTO {
  id: number;
  nome: string;
  tipo: TipoCliente;
  documento: string;
  telefone: string;
  email: string;
  origem: OrigemCliente | "";
  criadoEm: string; // ISO data-hora
}

export const CreateClienteRequestSchema = z.object({
  nome: texto(200).min(1, "Informe o nome do cliente."),
  tipo: z.enum(chaves(TIPOS_CLIENTE)).default("PF"),
  documento: texto(30).default(""),
  telefone: texto(40).default(""),
  email: texto(120).default(""),
  origem: z.enum([...chaves(ORIGENS_CLIENTE), ""]).default(""),
});
export type CreateClienteRequestDTO = z.infer<typeof CreateClienteRequestSchema>;

export const UpdateClienteRequestSchema = CreateClienteRequestSchema.extend({
  id: z.number({ message: "O id do cliente não pode ser nulo." }),
});
export type UpdateClienteRequestDTO = z.infer<typeof UpdateClienteRequestSchema>;

/** Linha da tela Clientes: cliente + resumo dos eventos. */
export interface ClienteListaItemResponseDTO extends ClienteResponseDTO {
  totalEventos: number;
  proximoEvento: string | null; // data do próximo evento ativo
}
