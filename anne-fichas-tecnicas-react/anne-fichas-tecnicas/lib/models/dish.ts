import { z } from "zod";
import { CATEGORIAS } from "./category";

/**
 * Ingrediente de uma ficha técnica.
 */
export const IngredienteSchema = z.object({
  nome: z.string().trim().min(1, "Nome do ingrediente é obrigatório").max(160),
  qtd: z.number().finite().nonnegative().max(1_000_000),
  unidade: z.string().trim().max(20).default(""),
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
  rendQtd: z.number().finite().positive().max(1_000_000),
  rendUnid: z.string().trim().min(1).max(30),
  porPessoa: z.number().finite().nonnegative().max(10_000),
  receita: z.string().trim().max(4000),
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
