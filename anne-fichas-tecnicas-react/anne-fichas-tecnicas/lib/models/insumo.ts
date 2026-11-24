import { z } from "zod";
import {
  CATEGORIAS_INSUMO,
  LOCAIS_ESTOQUE,
  TIPOS_ITEM,
  UNIDADES_INSUMO,
  chaves,
  type CategoriaInsumo,
  type LocalEstoque,
  type TipoItem,
  type UnidadeInsumo,
} from "./dominio";
import { dinheiro, idPositivo, texto } from "./comum";

/**
 * Item de estoque: ingrediente, embalagem, descartável, louça, utensílio ou
 * produto de limpeza. Um cadastro só serve às fichas, ao estoque e às notas.
 */
export interface InsumoResponseDTO {
  id: number;
  nome: string;
  tipo: TipoItem;
  categoria: CategoriaInsumo;
  unidade: UnidadeInsumo;
  local: LocalEstoque;
  unidadeCompra: string;
  fatorCompra: number | null;
  estoqueMinimo: number;
  custo: number;
  fornecedor: string;
  atualizadoEm: string; // "AAAA-MM-DD"
}

export const CreateInsumoRequestSchema = z.object({
  nome: texto(200).min(1, "Informe o nome."),
  tipo: z.enum(chaves(TIPOS_ITEM)).default("ingrediente"),
  categoria: z.enum(CATEGORIAS_INSUMO).default("Mercearia"),
  unidade: z.enum(chaves(UNIDADES_INSUMO)).default("kg"),
  local: z.enum(LOCAIS_ESTOQUE).default("Depósito"),
  unidadeCompra: texto(60).default(""),
  fatorCompra: z.number().positive().nullable().default(null),
  estoqueMinimo: z.number().min(0).default(0),
  custo: dinheiro().default(0),
  fornecedor: texto(120).default(""),
});
export type CreateInsumoRequestDTO = z.infer<typeof CreateInsumoRequestSchema>;
export const UpdateInsumoRequestSchema = CreateInsumoRequestSchema.extend({ id: idPositivo() });
export type UpdateInsumoRequestDTO = z.infer<typeof UpdateInsumoRequestSchema>;

/** Linha da aba Itens: cadastro + em quantas fichas aparece. */
export interface InsumoListaItemDTO extends InsumoResponseDTO {
  emFichas: number;
}
