import { z } from "zod";
import type { ItemPratoResponseDTO } from "./prato";

/**
 * Tipos espelhando, campo a campo, os DTOs de produção do backend real
 * (ProducaoController.java / ProducaoEntity.java).
 *
 * Paralelo a lib/models/prato.ts: este arquivo é aditivo e não substitui
 * lib/models/production.ts, que ainda serve a tela de Produção enquanto ela
 * roda sobre o backend de referência local (mock) — nada na UI foi migrado
 * para estes tipos ainda.
 */

export interface ProducaoResponseDTO {
  id: number;
  nome: string;
  quantidade: number;
  data: string; // LocalDate (ISO "AAAA-MM-DD")
  pratos: ItemPratoResponseDTO[];
}

export interface CreateProducaoItemRequestDTO {
  pratoId: number;
}

const CreateProducaoItemRequestSchema = z.object({
  pratoId: z.number({ message: "O ID do prato é obrigatório." }),
});

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Espelha CreateProducaoRequestDTO.java (@NotBlank, @Positive, @FutureOrPresent, @NotEmpty).
 */
export const CreateProducaoRequestSchema = z.object({
  nome: z
    .string()
    .trim()
    .min(1, "O nome do evento/produção é obrigatório.")
    .max(255, "O nome do evento deve ter no máximo 255 caracteres."),
  quantidade: z.number().int().positive("A quantidade de convidados deve ser maior que zero."),
  data: z
    .string()
    .regex(DATE_RE, "A data do evento não pode ser no passado."),
  pratos: z
    .array(CreateProducaoItemRequestSchema)
    .nonempty("A produção deve conter pelo menos um prato selecionado."),
});
export type CreateProducaoRequestDTO = z.infer<typeof CreateProducaoRequestSchema>;

/**
 * Espelha UpdateProducaoRequestDTO.java.
 */
export const UpdateProducaoRequestSchema = CreateProducaoRequestSchema.extend({
  id: z.number({ message: "O id do evento/produção não pode ser nulo." }),
});
export type UpdateProducaoRequestDTO = z.infer<typeof UpdateProducaoRequestSchema>;
