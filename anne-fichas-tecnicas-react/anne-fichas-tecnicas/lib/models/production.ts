import { z } from "zod";

/**
 * Item de uma produção: referência a um prato do catálogo + quantidade a produzir.
 */
export const ProductionItemSchema = z.object({
  dishId: z.string().min(1),
  quantidade: z.number().finite().positive().max(1_000_000),
});
export type ProductionItem = z.infer<typeof ProductionItemSchema>;

/**
 * Produção completa (lista de produção de um evento específico).
 */
export const ProductionSchema = z.object({
  id: z.string().min(1),
  evento: z.string().trim().min(1, "Nome do evento é obrigatório").max(200),
  data: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Data inválida (use AAAA-MM-DD)").optional().or(z.literal("")),
  convidados: z.number().finite().nonnegative().max(100_000),
  itens: z.array(ProductionItemSchema).min(1, "Adicione ao menos um prato com quantidade"),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
});
export type Production = z.infer<typeof ProductionSchema>;

export const CreateProductionInputSchema = ProductionSchema.omit({
  id: true,
  createdAt: true,
  updatedAt: true,
});
export type CreateProductionInput = z.infer<typeof CreateProductionInputSchema>;

export const UpdateProductionInputSchema = CreateProductionInputSchema.partial();
export type UpdateProductionInput = z.infer<typeof UpdateProductionInputSchema>;

/**
 * Linha calculada (join de item + prato) usada nas telas de cozinha/compras.
 * Não é persistida — é derivada em tempo de leitura.
 */
export interface ProductionKitchenRow {
  dishId: string;
  nome: string;
  categoria: string;
  foto?: string;
  quantidade: number;
  rendUnid: string;
  receita: string;
}

export interface ConsolidatedIngredientRow {
  nome: string;
  unidade: string;
  total: number;
}
