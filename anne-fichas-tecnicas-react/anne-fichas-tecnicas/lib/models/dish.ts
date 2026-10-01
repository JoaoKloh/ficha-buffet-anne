import { z } from "zod";
import { CATEGORIAS } from "./category";

/**
 * Ingrediente de uma ficha técnica. Vira um CreatePratoItemIngredienteDTO
 * (lib/models/prato.ts) ao enviar para o backend real — `qtd` é obrigatório e
 * maior que zero lá (@NotNull @Positive), por isso .positive() aqui também.
 */
export const IngredienteSchema = z.object({
  nome: z.string().trim().min(1, "Nome do ingrediente é obrigatório").max(160),
  qtd: z.number().finite().positive("A quantidade do ingrediente é obrigatória.").max(1_000_000),
  unidade: z.string().trim().max(20).default(""),
  // ID do ingrediente no catálogo real do backend (CreatePratoItemIngredienteDTO
  // exige "ingredienteId") — preenchido ao escolher no menu suspenso em
  // IngredientRows; ausente só se o ingrediente ainda não veio do catálogo real.
  ingredienteId: z.number().int().positive().optional(),
});
export type Ingrediente = z.infer<typeof IngredienteSchema>;

/**
 * Prato completo, como armazenado e retornado pela API.
 */
export const DishSchema = z.object({
  id: z.string().min(1),
  nome: z.string().trim().min(1, "Nome do prato é obrigatório").max(200),
  categoria: z.enum(CATEGORIAS),
  // Só aceitamos http(s) — nunca esquemas como javascript: ou data: em campos vindos do usuário.
  foto: z.string().trim().url().startsWith("https://").max(2000).optional().or(z.literal("")),
  // Integer no backend (CreatePratoRequestDTO#rendQtd, @Positive) — sem decimais.
  rendQtd: z.number().int().positive().max(1_000_000),
  rendUnid: z.string().trim().min(1).max(30),
  porPessoa: z.number().finite().nonnegative().max(10_000),
  // Máximo alinhado com CreatePratoRequestDTO#receita (@Size(max = 2000)).
  receita: z.string().trim().max(2000),
  ingredientes: z.array(IngredienteSchema).max(60),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
});
export type Dish = z.infer<typeof DishSchema>;

/**
 * Payload aceito ao criar um prato — sem id/timestamps (gerados no servidor).
 */
export const CreateDishInputSchema = DishSchema.omit({
  id: true,
  createdAt: true,
  updatedAt: true,
});
export type CreateDishInput = z.infer<typeof CreateDishInputSchema>;

/**
 * Payload de atualização — todos os campos opcionais (PATCH parcial).
 */
export const UpdateDishInputSchema = CreateDishInputSchema.partial();
export type UpdateDishInput = z.infer<typeof UpdateDishInputSchema>;
