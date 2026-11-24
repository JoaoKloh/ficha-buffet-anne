import { z } from "zod";
import { SECOES_CARTA, chaves, type SecaoCarta } from "./dominio";
import { dinheiro, idPositivo, texto } from "./comum";

/** Prato da carta do Restaurante ANNE, montado com fichas técnicas e insumos diretos. */
export interface PratoVendaResponseDTO {
  id: number;
  nome: string;
  secao: SecaoCarta;
  preco: number;
  codigoPdv: string;
  ativo: boolean;
  obs: string;
  componentes: { preparacaoId: number; qtd: number }[];
  extras: { insumoId: number; qtd: number }[];
  atualizadoEm: string;
}

export const CreatePratoVendaRequestSchema = z
  .object({
    nome: texto(200).min(1, "Informe o nome do prato."),
    secao: z.enum(chaves(SECOES_CARTA)).default("campo"),
    preco: dinheiro("O preço não pode ser negativo.").default(0),
    codigoPdv: texto(40).default(""),
    ativo: z.boolean().default(true),
    obs: texto(2000).default(""),
    componentes: z.array(z.object({ preparacaoId: idPositivo(), qtd: z.number().positive("Informe a quantidade por prato de cada item.") })).default([]),
    extras: z.array(z.object({ insumoId: idPositivo(), qtd: z.number().positive("Informe a quantidade por prato de cada item.") })).default([]),
  })
  .refine((v) => v.componentes.length + v.extras.length > 0, { message: "Ligue pelo menos uma ficha técnica ou insumo ao prato.", path: ["componentes"] });
export type CreatePratoVendaRequestDTO = z.infer<typeof CreatePratoVendaRequestSchema>;
export const UpdatePratoVendaRequestSchema = CreatePratoVendaRequestSchema.innerType().extend({ id: idPositivo() }).refine(
  (v) => v.componentes.length + v.extras.length > 0,
  { message: "Ligue pelo menos uma ficha técnica ou insumo ao prato.", path: ["componentes"] }
);
export type UpdatePratoVendaRequestDTO = z.infer<typeof UpdatePratoVendaRequestSchema>;

export interface CmvDTO {
  custo: number;
  faltando: number;
  cmv: number | null;
}

export interface PratoVendaListaItemDTO extends PratoVendaResponseDTO {
  cmv: CmvDTO;
  componentesNomes: string[];
}

export interface PratosVendaResponseDTO {
  kpis: { ativos: number; foraDaCarta: number; cmvMedio: number | null; acimaDaMeta: number; incompletos: number };
  pratos: PratoVendaListaItemDTO[];
}

export const CalcularCmvRequestSchema = z.object({
  componentes: z.array(z.object({ preparacaoId: z.number().int().positive(), qtd: z.number().min(0) })).default([]),
  extras: z.array(z.object({ insumoId: z.number().int().positive(), qtd: z.number().min(0) })).default([]),
  preco: z.number().min(0).default(0),
});
export type CalcularCmvRequestDTO = z.infer<typeof CalcularCmvRequestSchema>;
