import { z } from "zod";

/**
 * Sugestões de categoria para o <select> do formulário — o backend aceita
 * qualquer string não vazia (CreateIngredienteRequestDTO#categoria não é um
 * enum), então isto é só uma lista de conveniência, não uma restrição de tipo.
 */
export const CATEGORIAS_INGREDIENTE = [
  "Hortifruti",
  "Carnes e Aves",
  "Peixes e Frutos do Mar",
  "Laticínios e Frios",
  "Secos e Grãos",
  "Temperos e Condimentos",
  "Bebidas",
  "Descartáveis",
  "Outros",
] as const;

/**
 * Tipos espelhando, campo a campo, os DTOs de ingrediente do backend real
 * (IngredientesController.java / IngredientesEntity.java).
 */

/**
 * Como o backend devolve um ingrediente — GET /ingredientes/retornarTodos, e
 * também o formato de cada item em PratoDetalhadoResponseDTO.ingredientes
 * (ver lib/models/prato.ts, que reimporta este tipo em vez de duplicá-lo).
 * Espelha IngredienteResponseDTO.java campo a campo:
 * - Sem `descricao`: o record Java não a devolve, mesmo aceitando na
 *   criação/atualização — listar ou editar um ingrediente nunca traz a
 *   descrição de volta.
 * - `custo`/`fornecedor`: opcionais no cadastro (CreateIngredienteRequestDTO),
 *   por isso `null` quando o ingrediente foi criado sem esses dados.
 * - `qtd`: vem `null` na listagem do catálogo (GET /ingredientes/retornarTodos)
 *   e populado quando o item aparece dentro da ficha técnica de um prato
 *   (PratoDetalhadoResponseDTO.ingredientes, construído a partir de
 *   PratoIngredienteEntity — a tabela intermediária prato_ingrediente).
 */
export interface IngredienteResponseDTO {
  id: number;
  nome: string;
  qtd: number | null;
  unidade: string;
  categoria: string;
  custo: number | null;
  fornecedor: string | null;
}

/**
 * Espelha as validações Bean Validation de CreateIngredienteRequestDTO.java
 * (@NotBlank, @Size, @Positive).
 */
export const CreateIngredienteRequestSchema = z.object({
  nome: z
    .string()
    .trim()
    .min(1, "O nome do ingrediente é obrigatório.")
    .max(255, "O nome do ingrediente deve ter no máximo 255 caracteres."),
  unidade: z
    .string()
    .trim()
    .min(1, "A unidade de medida do ingrediente é obrigatória.")
    .max(50, "A unidade de medida deve ter no máximo 50 caracteres."),
  categoria: z.string().trim().min(1, "A categoria do ingrediente é obrigatória."),
  descricao: z
    .string()
    .trim()
    .max(1024, "A unidade de medida deve ter no máximo 1024 caracteres.")
    .optional(),
  custo: z.number().positive("O custo não pode ser negativo.").optional(),
  fornecedor: z.string().trim().max(50, "O fornecedo deve ter no máximo 50 caracteres.").optional(),
});
export type CreateIngredienteRequestDTO = z.infer<typeof CreateIngredienteRequestSchema>;

/**
 * Espelha UpdateIngredienteRequestDto.java — mesmo nome de classe do backend,
 * "to" minúsculo incluído. Não é um PATCH parcial: o backend exige os mesmos
 * campos da criação, mais o id (substituição completa).
 */
export const UpdateIngredienteRequestSchema = CreateIngredienteRequestSchema.extend({
  id: z.number({ message: "O id do ingrediente não pode ser nulo." }),
});
export type UpdateIngredienteRequestDto = z.infer<typeof UpdateIngredienteRequestSchema>;

/**
 * Espelha AssociationIngredientePratoRequestDTO.java (POST /ingredientes/associarPratos).
 */
export const AssociationIngredientePratoRequestSchema = z.object({
  ingredienteId: z.number({ message: "O ID do ingrediente é obrigatório." }),
  pratosId: z.array(z.number()).min(1, "É necessário vincular um ou mais pratos."),
});
export type AssociationIngredientePratoRequestDTO = z.infer<
  typeof AssociationIngredientePratoRequestSchema
>;
