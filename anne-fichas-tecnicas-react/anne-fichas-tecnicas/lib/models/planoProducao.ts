import { z } from "zod";
import { STATUS_PLANO, UNIDADES_PLANO, chaves, type StatusPlano, type UnidadePlano } from "./dominio";
import { dataIso, idPositivo, texto } from "./comum";
import type { FichaStatus } from "./preparacao";

/**
 * Planos de produção do Restaurante ANNE e da Chocolates Marie & Anne, separados dos eventos do buffet.
 * Concluir um item cria um lote em Estoque › Pré-preparos com baixa de insumos: é o lote que baixa
 * o estoque, uma vez só.
 */

export interface ItemPlanoDTO {
  /** Chave do item dentro do plano. */
  k: string;
  preparacaoId: number;
  /** Na unidade de rendimento da ficha. */
  qtd: number;
  feito: boolean;
  feitoEm: string | null;
  loteId: number | null;
}

export interface PlanoProducaoResponseDTO {
  id: number;
  titulo: string;
  unidade: UnidadePlano;
  status: StatusPlano;
  dataInicio: string;
  dataFim: string;
  obs: string;
  itens: ItemPlanoDTO[];
  /** Pratos da carta previstos no período ("Calcular pela carta"): pratoVendaId → quantidade. */
  previsao: Record<string, number>;
  criadoEm: string;
  atualizadoEm: string;
}

/* --------------------------------- Telas --------------------------------- */

export interface PlanoListaItemDTO {
  id: number;
  titulo: string;
  unidade: UnidadePlano;
  status: StatusPlano;
  dataInicio: string;
  dataFim: string;
  itens: number;
  feitos: number;
}

export interface PlanosProducaoResponseDTO {
  unidade: UnidadePlano;
  kpis: { abertos: number; itensAProduzir: number; concluidos: number; prePreparosProntos: number };
  /** Abertos primeiro, depois os mais recentes. */
  planos: PlanoListaItemDTO[];
}

export interface LinhaPlanoDTO {
  k: string;
  preparacaoId: number;
  /** null quando a ficha foi removida. */
  nome: string | null;
  categoria: string;
  categoriaLabel: string;
  praca: string;
  qtd: number;
  /** Unidade da ficha já no rótulo certo ("porções", "un", "kg"). */
  unidade: string;
  receitas: number;
  rendimento: number;
  fichaStatus: FichaStatus | "Ficha removida";
  validadeDias: number | null;
  feito: boolean;
  feitoEm: string | null;
  lote: { id: number; codigo: string } | null;
}

export interface InsumosItemPlanoDTO {
  k: string;
  nome: string;
  categoriaLabel: string;
  praca: string;
  qtd: number;
  unidade: string;
  rendimentoLabel: string;
  feito: boolean;
  modoPreparo: string;
  /** Por receita (qb) e total do item (qt), com sub-receitas abertas ("via"). */
  ins: { n: string; u: string; via: string; qb: number; qt: number }[];
}

export interface CompraPlanoDTO {
  insumoId: number;
  nome: string;
  unidade: string;
  precisa: number;
  estoque: number;
  falta: number;
  custo: number;
  semPreco: boolean;
  usos: string[];
}

export interface PlanoDetalheResponseDTO {
  plano: PlanoProducaoResponseDTO;
  linhas: LinhaPlanoDTO[];
  insumos: InsumosItemPlanoDTO[];
  /** Só os itens ainda não concluídos. */
  compras: { kpis: { necessarios: number; aComprar: number; custo: number; semPreco: number; concluidos: number }; itens: CompraPlanoDTO[] };
}

/* -------------------------------- Requests -------------------------------- */

export const SalvarPlanoRequestSchema = z
  .object({
    titulo: texto(160).min(1, "Dê um nome ao plano."),
    unidade: z.enum(chaves(UNIDADES_PLANO)),
    status: z.enum(chaves(STATUS_PLANO)).default("planejado"),
    dataInicio: dataIso("Informe a data de início."),
    dataFim: dataIso("Informe a data final."),
    obs: texto(2000).default(""),
  })
  .refine((v) => v.dataFim >= v.dataInicio, { message: "A data final não pode ser antes do início.", path: ["dataFim"] });
export type SalvarPlanoRequestDTO = z.infer<typeof SalvarPlanoRequestSchema>;

export const AdicionarItensPlanoRequestSchema = z.object({
  itens: z.array(z.object({ preparacaoId: idPositivo(), qtd: z.number().positive("Informe a quantidade de todos os itens marcados.") })).min(1, "Marque ao menos um item."),
});
export type AdicionarItensPlanoRequestDTO = z.infer<typeof AdicionarItensPlanoRequestSchema>;

export const AjustarItemPlanoRequestSchema = z.object({ qtd: z.number().positive("Informe uma quantidade maior que zero.") });
export type AjustarItemPlanoRequestDTO = z.infer<typeof AjustarItemPlanoRequestSchema>;

/** Pratos da carta previstos → fichas que eles levam entram (ou atualizam) no plano. */
export const CalcularCartaRequestSchema = z.object({
  previsao: z.record(z.string().regex(/^\d+$/), z.number().int().min(0)).refine((p) => Object.values(p).some((n) => n > 0), "Informe ao menos um prato previsto."),
});
export type CalcularCartaRequestDTO = z.infer<typeof CalcularCartaRequestSchema>;

export const ItemPlanoAcaoRequestSchema = z.object({ k: z.string().min(1) });
export type ItemPlanoAcaoRequestDTO = z.infer<typeof ItemPlanoAcaoRequestSchema>;
