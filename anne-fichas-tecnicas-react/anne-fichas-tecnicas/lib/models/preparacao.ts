import { z } from "zod";
import {
  CATEGORIAS_PREPARACAO,
  CONSERVACOES,
  PRACAS,
  UNIDADES_COMPONENTE,
  UNIDADES_RENDIMENTO,
  chaves,
  type CategoriaPreparacao,
  type Conservacao,
  type Praca,
  type UnidadeComponente,
  type UnidadeRendimento,
} from "./dominio";
import { idPositivo, texto } from "./comum";

/** Linha da receita: um insumo do cadastro ou uma sub-receita (outra ficha). */
export interface ItemPreparacaoDTO {
  insumoId: number | null;
  subId: number | null;
  qtd: number;
  /** Fator de correção da limpeza: 1 sem perda, 1,3 compra 30% a mais. */
  fc: number;
}
export interface LoucaPreparacaoDTO {
  insumoId: number;
  qtd: number;
}
export interface EtapaPreparacaoDTO {
  desc: string;
  praca: Praca | "";
  /** Dias antes do evento; null = a posicionar. */
  dn: number | null;
}
export interface ComponentePreparacaoDTO {
  n: string;
  u: UnidadeComponente;
  q: number;
  cons: Conservacao;
  obs?: string;
}

export interface PreparacaoResponseDTO {
  id: number;
  nome: string;
  categoria: CategoriaPreparacao;
  praca: Praca;
  conservacao: Conservacao;
  validadeDias: number | null;
  /** Setor de produção das ordens ("" = automático pela praça). */
  setor?: string;
  /** Antecedência da produção em dias (null = padrão da praça: Frios D-7, Quentes D-6, Salgados D-5). */
  dnProducao?: number | null;
  /** Tempo por receita, em minutos. */
  tempoMin?: number | null;
  equipamentos?: string;
  armazenamento?: string;
  /** Comprado pronto do fornecedor: a produção não faz, só retira do estoque e finaliza. */
  compradoPronto?: boolean;
  rendimento: number;
  unidadeRendimento: UnidadeRendimento;
  porcaoPadrao: number;
  itens: ItemPreparacaoDTO[];
  loucas: LoucaPreparacaoDTO[];
  etapas: EtapaPreparacaoDTO[];
  componentes: ComponentePreparacaoDTO[];
  obsEnvio: string;
  modoPreparo: string;
  atualizadoEm: string;
}

const ItemSchema = z
  .object({
    insumoId: z.number().int().positive().nullable().default(null),
    subId: z.number().int().positive().nullable().default(null),
    qtd: z.number().positive("Informe a quantidade de cada item da receita."),
    fc: z.number().positive().default(1),
  })
  .refine((it) => (it.insumoId != null) !== (it.subId != null), "Cada item é um insumo ou uma sub-receita.");

export const CreatePreparacaoRequestSchema = z.object({
  nome: texto(200).min(1, "Informe o nome da preparação."),
  categoria: z.enum(chaves(CATEGORIAS_PREPARACAO)),
  praca: z.enum(PRACAS),
  conservacao: z.enum(chaves(CONSERVACOES)).default("refrigerado"),
  validadeDias: z.number().int().min(0).nullable().default(null),
  setor: texto(40).default(""),
  dnProducao: z.number().int().min(0).max(30).nullable().default(null),
  tempoMin: z.number().int().min(0).nullable().default(null),
  equipamentos: texto(200).default(""),
  armazenamento: texto(300).default(""),
  compradoPronto: z.boolean().default(false),
  rendimento: z.number().positive("Informe quanto a receita rende."),
  unidadeRendimento: z.enum(chaves(UNIDADES_RENDIMENTO)).default("porções"),
  porcaoPadrao: z.number().min(0).default(1),
  itens: z.array(ItemSchema).default([]),
  loucas: z.array(z.object({ insumoId: idPositivo(), qtd: z.number().positive("Informe a quantidade de cada louça.") })).default([]),
  etapas: z
    .array(z.object({ desc: texto(300).min(1), praca: z.enum([...PRACAS, ""]).default(""), dn: z.number().int().min(0).nullable().default(null) }))
    .default([]),
  componentes: z
    .array(
      z.object({
        n: texto(200).min(1),
        u: z.enum(chaves(UNIDADES_COMPONENTE)).default("kg"),
        q: z.number().positive("Informe a quantidade de cada componente de envio."),
        cons: z.enum(chaves(CONSERVACOES)).default("ambiente"),
        obs: texto(300).optional(),
      })
    )
    .default([]),
  obsEnvio: texto(300).default(""),
  modoPreparo: texto(4000).default(""),
});
export type CreatePreparacaoRequestDTO = z.infer<typeof CreatePreparacaoRequestSchema>;
export const UpdatePreparacaoRequestSchema = CreatePreparacaoRequestSchema.extend({ id: idPositivo() });
export type UpdatePreparacaoRequestDTO = z.infer<typeof UpdatePreparacaoRequestSchema>;

export type FichaStatus = "Completa" | "Sem preço" | "Incompleta";

export interface CustoPreparacaoDTO {
  total: number;
  porUnid: number;
  faltando: number;
}

/** Linha da aba Preparações: ficha + custo calculado + situação. */
export interface PreparacaoListaItemDTO extends PreparacaoResponseDTO {
  custo: CustoPreparacaoDTO;
  fichaStatus: FichaStatus;
  usadaEmCardapio: boolean;
  usadaComoSub: string[];
}

/** Rascunho enviado para calcular o custo ao vivo no formulário. */
export const CalcularCustoRequestSchema = z.object({
  itens: z
    .array(z.object({ insumoId: z.number().int().positive().nullable().default(null), subId: z.number().int().positive().nullable().default(null), qtd: z.number().min(0).default(0), fc: z.number().positive().default(1) }))
    .default([]),
  rendimento: z.number().positive().default(1),
});
export type CalcularCustoRequestDTO = z.infer<typeof CalcularCustoRequestSchema>;

/** Opções dos formulários de ficha e de prato de venda. */
export interface FichasOpcoesResponseDTO {
  /** Consumíveis agrupados por setor de compra: [id, "Nome (unidade)"]. */
  insumos: { grupo: string; itens: [number, string][] }[];
  /** Louças e utensílios. */
  reutilizaveis: [number, string][];
  /** Todas as fichas, para sub-receita e prato de venda: base primeiro. */
  preparacoes: { id: number; nome: string; categoria: string; unidade: string; porcaoPadrao: number }[];
  /** Unidade de estoque de cada insumo. */
  unidades: Record<number, string>;
  /** Preparações agrupadas por categoria (para selects). */
  preparacoesAgrupadas: { grupo: string; itens: [number, string][] }[];
  /** Setores de produção ativos: [id, nome] (sem eles, a tela usa os setores padrão). */
  setores?: [string, string][];
}
