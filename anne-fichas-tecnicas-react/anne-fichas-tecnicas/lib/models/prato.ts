import { z } from "zod";
import type { IngredienteResponseDTO } from "./ingredient";

/**
 * Tipos espelhando, campo a campo, os DTOs de com.jbkloh.ficha_buffet no
 * backend real (PratoController.java) — nomes e tipos mantidos idênticos aos
 * records Java para que o payload trafegue sem tradução entre as camadas.
 */

export type { IngredienteResponseDTO };

/**
 * Regras do upload da foto do prato (Vercel Blob). Validadas no browser antes
 * do envio e repetidas no token gerado por app/api/prato/upload/route.ts, para
 * que o Blob recuse o arquivo mesmo se a checagem do cliente for contornada.
 * O backend só recebe a URL resultante, no campo `foto`.
 */
export const FOTO_MAX_BYTES = 5 * 1024 * 1024;
export const FOTO_TIPOS_ACEITOS = ["image/jpeg", "image/png", "image/webp", "image/gif", "image/avif"];

/** Mensagem de erro para o arquivo escolhido, ou null se ele pode ser enviado. */
export function validarFoto(file: File): string | null {
  if (!FOTO_TIPOS_ACEITOS.includes(file.type)) {
    return "O arquivo deve ser uma imagem (JPG, PNG, WebP, GIF ou AVIF).";
  }
  if (file.size > FOTO_MAX_BYTES) {
    return "A imagem deve ter no máximo 5 MB.";
  }
  return null;
}

export interface PratoDetalhadoResponseDTO {
  id: number;
  nome: string;
  categoria: string;
  rendQtd: number;
  rendUnid: string;
  porPessoa: number;
  receita: string;
  foto: string;
  // Cada item vem no formato de IngredienteResponseDTO (id, nome, qtd,
  // unidade, categoria, custo, fornecedor) com `qtd` sempre preenchido:
  // PratoEntity.itensIngredientes é a tabela intermediária prato_ingrediente
  // (PratoIngredienteEntity), que guarda a quantidade daquele ingrediente
  // especificamente nesta receita.
  ingredientes: IngredienteResponseDTO[];
}

export interface CreatePratoItemIngredienteDTO {
  ingredienteId: number;
  qtd: number;
  unidade?: string;
}

/**
 * Espelha ItemPratoResponseDTO.java — versão resumida do prato (sem
 * ingredientes), usada dentro de ProducaoResponseDTO#pratos (ver
 * lib/models/producao.ts).
 */
export interface ItemPratoResponseDTO {
  id: number;
  nome: string;
  categoria: string;
  rendQtd: number;
  rendUnid: string;
  porPessoa: number;
  receita: string;
  foto: string;
}

/**
 * Espelha CreatePratoItemIngredienteDTO.java — usado tanto em
 * CreatePratoRequestDTO quanto em UpdatePratoRequest#ingredientes. Desde que
 * o backend passou a ter a tabela intermediária prato_ingrediente
 * (PratoIngredienteEntity), a quantidade é por linha de ingrediente, não
 * mais um valor único no prato — ver o histórico de CreatePratoRequestSchema
 * abaixo.
 */
const CreatePratoItemIngredienteSchema = z.object({
  ingredienteId: z.number({ message: "O ID do ingrediente é obrigatório." }),
  qtd: z
    .number({ message: "A quantidade do ingrediente é obrigatória." })
    .positive("A quantidade do ingrediente deve ser maior que zero."),
  unidade: z
    .string()
    .trim()
    .max(50, "A unidade do ingrediente deve ter no máximo 50 caracteres.")
    .optional(),
});

/**
 * Espelha as validações Bean Validation de CreatePratoRequestDTO.java
 * (@NotBlank, @Size, @Positive, @Min, @NotEmpty).
 *
 * Sem campo `qtd` no nível do prato: CreatePratoRequestDTO.java nunca teve
 * esse campo — a quantidade é só a de cada `CreatePratoItemIngredienteDTO`
 * dentro de `ingredientes` (ver acima).
 */
export const CreatePratoRequestSchema = z.object({
  nome: z
    .string()
    .trim()
    .min(1, "O nome do prato é obrigatório.")
    .max(255, "O nome do prato deve ter no máximo 255 caracteres."),
  categoria: z
    .string()
    .trim()
    .min(1, "A categoria é obrigatória.")
    .max(100, "A categoria deve ter no máximo 100 caracteres."),
  foto: z.string().trim().url("A foto deve ser uma URL válida.").nullable(),
  rendQtd: z
    .number()
    .int()
    .positive("O rendimento da receita deve ser maior que zero."),
  rendUnid: z
    .string()
    .trim()
    .min(1, "A unidade do rendimento é obrigatória.")
    .max(50, "A unidade do rendimento deve ter no máximo 50 caracteres."),
  // Double no backend (@Min(0)) — aceita decimais (ex.: 1.5 por pessoa), sem .int().
  porPessoa: z.number().min(0, "A sugestão por pessoa não pode ser negativa."),
  receita: z.string().max(2000, "A receita/modo de preparo deve ter no máximo 2000 caracteres."),
  ingredientes: z
    .array(CreatePratoItemIngredienteSchema)
    .nonempty("O prato deve conter pelo menos um ingrediente."),
});
export type CreatePratoRequestDTO = z.infer<typeof CreatePratoRequestSchema>;

/**
 * Espelha UpdatePratoRequest.java — mesmos campos de CreatePratoRequestDTO
 * mais o `id`; também sem `qtd` no nível do prato.
 */
export const UpdatePratoRequestSchema = z.object({
  id: z.number({ message: "O id do prato precisa ser informado." }),
  nome: z
    .string()
    .trim()
    .min(1, "O nome do prato é obrigatório.")
    .max(255, "O nome do prato deve ter no máximo 255 caracteres."),
  categoria: z
    .string()
    .trim()
    .min(1, "A categoria é obrigatória.")
    .max(100, "A categoria deve ter no máximo 100 caracteres."),
  foto: z.string().trim().url("A foto deve ser uma URL válida.").nullable(),
  rendQtd: z
    .number()
    .int()
    .positive("O rendimento da receita deve ser maior que zero."),
  rendUnid: z
    .string()
    .trim()
    .min(1, "A unidade do rendimento é obrigatória.")
    .max(50, "A unidade do rendimento deve ter no máximo 50 caracteres."),
  // Double no backend (@Min(0)) — aceita decimais, sem .int().
  porPessoa: z.number().min(0, "A sugestão por pessoa não pode ser negativa."),
  receita: z.string().max(2000, "A receita/modo de preparo deve ter no máximo 2000 caracteres."),
  ingredientes: z
    .array(CreatePratoItemIngredienteSchema)
    .nonempty("O prato deve conter pelo menos um ingrediente."),
});
export type UpdatePratoRequest = z.infer<typeof UpdatePratoRequestSchema>;

/**
 * Espelha AssociationPratoProducaoRequestDTO.java (POST /prato/associarEventos).
 */
export const AssociationPratoProducaoRequestSchema = z.object({
  pratoId: z.number({ message: "O ID do prato é obrigatório." }),
  eventosId: z.array(z.number()).min(1, "É necessário vincular um ou mais eventos."),
});
export type AssociationPratoProducaoRequestDTO = z.infer<
  typeof AssociationPratoProducaoRequestSchema
>;
