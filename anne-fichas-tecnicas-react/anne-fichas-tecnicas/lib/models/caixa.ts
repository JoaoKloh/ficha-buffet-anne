import { z } from "zod";
import {
  CONTAS,
  CUSTO_TIPOS,
  FORMAS_PAGAMENTO_NF,
  GRUPOS_DRE,
  UNIDADES,
  chaves,
  type Conta,
  type CustoTipo,
  type FormaPagamentoNf,
  type GrupoDre,
  type OrigemLancamento,
  type StatusLancamento,
  type TipoLancamento,
  type Unidade,
} from "./dominio";
import { dataIso, dinheiroPositivo, idPositivo, texto } from "./comum";

/* -------------------------------- Categoria -------------------------------- */

export interface CategoriaResponseDTO {
  id: number;
  nome: string;
  grupo: GrupoDre;
  ordem: number;
  /** Como a categoria conta no previsto × realizado dos eventos; null = automático pelo nome. */
  custoEvento?: CustoTipo | null;
}

export const CreateCategoriaRequestSchema = z.object({
  nome: texto(100).min(1, "Informe o nome."),
  grupo: z.enum(chaves(GRUPOS_DRE)),
  ordem: z.number().int().min(0).default(0),
  custoEvento: z.enum(chaves(CUSTO_TIPOS)).nullable().default(null),
});
export type CreateCategoriaRequestDTO = z.infer<typeof CreateCategoriaRequestSchema>;
export const UpdateCategoriaRequestSchema = CreateCategoriaRequestSchema.extend({ id: idPositivo() });
export type UpdateCategoriaRequestDTO = z.infer<typeof UpdateCategoriaRequestSchema>;

/* ------------------------------- Lançamento -------------------------------- */

/** Um pagamento ou recebimento: em = quando foi registrado, por = quem registrou. */
export interface PagamentoDTO {
  id: number | null;
  data: string;
  valor: number;
  conta: Conta;
  forma: string;
  em: string | null;
  por: string;
}

export interface LancamentoResponseDTO {
  id: number;
  tipo: TipoLancamento;
  categoriaId: number;
  eventoId: number | null;
  descricao: string;
  valor: number;
  dataCompetencia: string;
  dataPagamento: string | null;
  vencimento: string | null;
  status: StatusLancamento;
  origem: OrigemLancamento;
  conta: Conta;
  parcelaId: number | null;
  extraId: number | null;
  nfId: number | null;
  parcelaNF: number | null;
  formaPag: FormaPagamentoNf | null;
  fornecedor: string | null;
  criadoEm: string;
  /** Unidade gravada; nula = ainda sem unidade (veja unidadeEfetiva na lista). */
  unidade: Unidade | null;
  /** Transferência entre unidades. */
  unidadeOrigem: Unidade | null;
  unidadeDestino: Unidade | null;
  insumoId: number | null;
  item: string | null;
  qtd: number | null;
  unid: string | null;
  responsavel: string | null;
  obs: string | null;
  /** Histórico de pagamentos ou recebimentos, do mais antigo para o mais recente. */
  pagamentos: PagamentoDTO[];
  criadoPor: string | null;
  alteradoPor: string | null;
  alteradoEm: string | null;
}

export const CreateLancamentoRequestSchema = z.object({
  descricao: texto(255).min(1, "Descreva o lançamento."),
  valor: dinheiroPositivo("Informe o valor."),
  categoriaId: idPositivo("Escolha a categoria."),
  eventoId: z.number().int().positive().nullable(),
  dataCompetencia: dataIso("Informe a competência."),
  status: z.enum(["previsto", "realizado"]),
  vencimento: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).nullable(),
  dataPagamento: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).nullable(),
  conta: z.enum(chaves(CONTAS)).default("banco"),
  unidade: z.enum(chaves(UNIDADES), { message: "Escolha a unidade de negócio." }),
  fornecedor: texto(120).nullable(),
});
export type CreateLancamentoRequestDTO = z.infer<typeof CreateLancamentoRequestSchema>;
export const UpdateLancamentoRequestSchema = CreateLancamentoRequestSchema.extend({ id: idPositivo() });
export type UpdateLancamentoRequestDTO = z.infer<typeof UpdateLancamentoRequestSchema>;

/** Pagamento ou recebimento de uma parte (ou do total). */
export const PagamentoRequestSchema = z.object({
  valor: dinheiroPositivo("Informe o valor."),
  data: dataIso("Informe a data."),
  conta: z.enum(chaves(CONTAS)),
  forma: z.union([z.enum(chaves(FORMAS_PAGAMENTO_NF)), z.literal("")]),
});
export type PagamentoRequestDTO = z.infer<typeof PagamentoRequestSchema>;

/** Transferência entre unidades: uma unidade usou produto ou insumo comprado por outra. */
export const TransferenciaRequestSchema = z.object({
  data: dataIso("Informe a data."),
  responsavel: texto(100),
  unidadeOrigem: z.enum(chaves(UNIDADES)),
  unidadeDestino: z.enum(chaves(UNIDADES)),
  insumoId: z.number().int().positive().nullable(),
  item: texto(200),
  qtd: z.number().min(0).nullable(),
  unid: texto(20),
  valor: dinheiroPositivo("Informe o custo da transferência."),
  categoriaId: idPositivo("Escolha a linha de custo."),
  obs: texto(500),
});
export type TransferenciaRequestDTO = z.infer<typeof TransferenciaRequestSchema>;
export const UpdateTransferenciaRequestSchema = TransferenciaRequestSchema.extend({ id: idPositivo() });
export type UpdateTransferenciaRequestDTO = z.infer<typeof UpdateTransferenciaRequestSchema>;

export const ClassificarRequestSchema = z.object({
  unidade: z.enum(chaves(UNIDADES), { message: "Escolha a unidade." }),
  ids: z.array(idPositivo()).min(1, "Nenhum lançamento para classificar."),
});
export type ClassificarRequestDTO = z.infer<typeof ClassificarRequestSchema>;

export const SaldosRequestSchema = z.object({
  data: dataIso("Informe a data."),
  contas: z.record(z.enum(chaves(CONTAS)), z.number().nullable()),
});
export type SaldosRequestDTO = z.infer<typeof SaldosRequestSchema>;

export const RateioRequestSchema = z.object({
  buffet: z.number().min(0),
  restaurante: z.number().min(0),
  chocolates: z.number().min(0),
});
export type RateioRequestDTO = z.infer<typeof RateioRequestSchema>;

/* ------------------------------ Tela do Caixa ------------------------------ */

export const ABAS_CAIXA = [
  ["lanc", "Lançamentos"],
  ["pagar", "Contas a pagar"],
  ["receber", "Contas a receber"],
  ["fluxo", "Fluxo de caixa"],
  ["transf", "Transferências"],
] as const;
export type AbaCaixa = (typeof ABAS_CAIXA)[number][0];

export const FILTROS_CAIXA = [
  ["todos", "Todos"],
  ["receita", "Receitas"],
  ["despesa", "Despesas"],
  ["previsto", "Em aberto"],
  ["realizado", "Pagos e recebidos"],
] as const;
export type FiltroCaixa = (typeof FILTROS_CAIXA)[number][0];

export const FILTROS_CONTAS = [
  ["abertas", "Em aberto"],
  ["vencidas", "Vencidas"],
  ["semana", "Vencem em 7 dias"],
  ["parciais", "Pagas em parte"],
  ["quitadas", "Quitadas (60 dias)"],
] as const;
export type FiltroContas = (typeof FILTROS_CONTAS)[number][0];

/** Filtro de unidade das abas do Caixa: "" todas, uma unidade ou "_sem". */
export const FILTROS_UNIDADE = [
  ["", "Todas as unidades"],
  ["buffet", "Buffet"],
  ["restaurante", "Restaurante"],
  ["chocolates", "Chocolates"],
  ["compartilhado", "Compartilhado"],
  ["_sem", "Sem unidade"],
] as const;
export type FiltroUnidade = (typeof FILTROS_UNIDADE)[number][0];

/** Lançamento com os nomes e os valores que as listas mostram. */
export interface LancamentoListaItemDTO extends LancamentoResponseDTO {
  grupo: GrupoDre;
  categoriaNome: string | null;
  eventoNome: string | null;
  /** "" = sem unidade (transferências também vêm vazias). */
  unidadeEfetiva: Unidade | "";
  /** Data do evento para lançamentos de evento; senão a competência lançada. */
  competencia: string;
  vencimentoEfetivo: string;
  pago: number;
  saldo: number;
  situacaoTom: "" | "good" | "warn" | "bad";
  situacao: string;
  /** Cliente (receita de evento) ou fornecedor. */
  parte: string;
}

export interface CaixaMesResponseDTO {
  mes: string; // "AAAA-MM"
  unidade: FiltroUnidade;
  kpis: {
    receitas: number;
    despesas: number;
    /** Saldo em aberto das receitas e das despesas do mês. */
    aReceber: number;
    aPagar: number;
    resultado: number;
    margem: number | null;
    totalLancamentos: number;
  };
  lancamentos: LancamentoListaItemDTO[];
  categorias: (CategoriaResponseDTO & { totalLancamentos: number })[];
  eventos: { id: number; nome: string; dataEvento: string }[];
  semUnidade: number;
}

export interface ContasResponseDTO {
  tipo: "pagar" | "receber";
  filtro: FiltroContas;
  unidade: FiltroUnidade;
  emAberto: { valor: number; n: number };
  vencido: { valor: number; n: number };
  seteDias: { valor: number; n: number };
  parcial: { pago: number; saldo: number; n: number };
  contas: LancamentoListaItemDTO[];
  semUnidade: number;
}

export interface FluxoCaixaResponseDTO {
  unidade: FiltroUnidade;
  mes: string;
  contas: { conta: Conta; rotulo: string; saldo: number | null; informado: number | null; informadoEm: string | null }[];
  temSaldo: boolean;
  saldoTotal: number;
  recebidoMes: number;
  pagoMes: number;
  entradas30: number;
  saidas30: number;
  projetado30: number | null;
  linhas: { rotulo: string; entradas: number; saidas: number; liquido: number; saldo: number | null; vencidos: boolean }[];
  saldosData: string | null;
  semUnidade: number;
}

export interface TransferenciasResponseDTO {
  unidade: FiltroUnidade;
  mes: string;
  porUnidade: { unidade: Unidade; recebeu: number; enviou: number; liquido: number }[];
  total: number;
  doMes: number;
  transferencias: LancamentoListaItemDTO[];
  semUnidade: number;
}

export interface SemUnidadeResponseDTO {
  total: number;
  lancamentos: LancamentoListaItemDTO[];
}

/* --------------------------------- Tela DRE -------------------------------- */

export interface DreLinhaDTO {
  /** sum = subtotal, final = resultado líquido, grupo = linha de dedução, cat = categoria, fora = investimentos (fora do resultado). */
  tipo: "sum" | "grupo" | "cat" | "final" | "fora";
  rotulo: string;
  valor: number;
  /** Fração da receita bruta (null sem receita). */
  pct: number | null;
}

export const UNIDADES_DRE = [
  ["", "Consolidado (todas as unidades)"],
  ["buffet", "Buffet Anne"],
  ["restaurante", "Restaurante ANNE"],
  ["chocolates", "Chocolates Marie & Anne"],
  ["compartilhado", "Estrutura compartilhada (sem rateio)"],
  ["_sem", "Sem unidade"],
  ["_cmp", "Comparar as unidades"],
] as const;
export type UnidadeDre = (typeof UNIDADES_DRE)[number][0];
export type RegimeDre = "competencia" | "caixa";

export interface DreResponseDTO {
  ano: string;
  periodo: string; // "ano" ou "01".."12"
  incluirPrevistos: boolean;
  titulo: string;
  anos: string[];
  /** Vazias no comparativo. */
  linhas: DreLinhaDTO[];
  resultadoPorEvento: { id: number; nome: string; dataEvento: string; rec: number; desp: number; res: number; mg: number | null }[];
  unidade: UnidadeDre;
  regime: RegimeDre;
  ratear: boolean;
  descricaoRegime: string;
  /** Percentual de cada unidade operacional no rateio. */
  rateio: Record<"buffet" | "restaurante" | "chocolates", number>;
  rateioDefinido: boolean;
  semUnidade: number;
  comparativo: {
    colunas: { chave: string; rotulo: string }[];
    linhas: { tipo: "sum" | "grupo" | "final"; rotulo: string; valores: number[] }[];
    margens: (number | null)[];
    investimentos: number[] | null;
  } | null;
  /** Categorias com "investimento" no nome que estão numa linha do resultado. */
  categoriasInvestimento: string[];
  resultado: number;
}
