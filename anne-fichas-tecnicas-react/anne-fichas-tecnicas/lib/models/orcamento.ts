import { z } from "zod";

/** Módulo Orçamento: precificação de um evento (custos → margens → preço de venda). */

export const CATEGORIAS_EVENTO = [
  "Casamento",
  "Debutante / 15 anos",
  "Corporativo",
  "Aniversário",
  "Batizado",
  "Formatura",
  "Outro",
] as const;

export interface DadosEvento {
  nome: string;
  /** ISO yyyy-mm-dd */
  data: string;
  local: string;
  cliente: string;
  contato: string;
  tipoServico: string;
  categoria: string;
  pax: number;
  duracaoHoras: number;
  /** hh:mm */
  horario: string;
}

/** Item do cardápio: custo = valorUnit × porções por pessoa × base (convidados ou equipe). */
export interface ItemCardapio {
  nome: string;
  gramas: number;
  valorUnit: number;
  porcaoPax: number;
  incluir: boolean;
}

export interface CategoriaCardapio {
  categoria: string;
  /** "staff": a quantidade base é o tamanho da equipe, não o nº de convidados. */
  baseQtd: "pax" | "staff";
  itens: ItemCardapio[];
}

/** Item com quantidade × valor unitário (bebidas, gelo, descartáveis, louças, pacote). */
export interface ItemQtdValor {
  nome: string;
  qtd: number;
  valorUnit: number;
  incluir: boolean;
}

export interface GrupoItens {
  nome: string;
  itens: ItemQtdValor[];
}

export interface FuncaoEquipe {
  funcao: string;
  /** Convidados por profissional; usado quando `auto` está ligado. */
  profissionalPorPax: number;
  qtd: number;
  diarias: number;
  valorUnit: number;
  auto: boolean;
}

/** Linha de custo sempre incluída (verba extra, degustação). */
export interface ItemCusto {
  nome: string;
  qtd: number;
  valorUnit: number;
}

export interface ServicoAdicional {
  nome: string;
  empresa: string;
  custo: number;
  valorCobrado: number;
}

export type ChaveInsumo = "alimentos" | "bebida" | "gelo" | "descartaveis";

/** Percentuais (0–100) aplicados na formação do preço. */
export interface Margens {
  margemBruta: number;
  comissao: number;
  outros: number;
  impostos: number;
  despesasFixas: number;
  margemLucroBruto: number;
  margemInvestimento: number;
}

export interface Orcamento {
  evento: DadosEvento;
  /** Verba planejada por convidado (R$) de cada insumo. */
  verbaInsumos: Record<ChaveInsumo, number>;
  cardapio: CategoriaCardapio[];
  bebidas: GrupoItens[];
  gelo: ItemQtdValor[];
  descartaveis: GrupoItens[];
  equipe: FuncaoEquipe[];
  materiais: {
    frete: number;
    verbaLocacao: number;
    grupos: GrupoItens[];
  };
  verbaExtra: ItemCusto[];
  degustacao: ItemCusto[];
  servicosAdicionais: ServicoAdicional[];
  pacoteBebidas: {
    itens: ItemQtdValor[];
    valorVendaPorPessoa: number;
  };
  margens: Margens;
  notas: string;
}

/**
 * Espelha, campo a campo, OrcamentoDetalhadoDTO.java (resposta de /orcamento).
 * Valores monetários são BigDecimal no backend (2 casas decimais).
 */
export interface OrcamentoDetalhadoDTO {
  id: number;
  nomeCliente: string;
  contatoCliente: string | null;
  tipoServico: string | null;
  /** Horas */
  duracaoEvento: number;
  numeroConvidados: number;
  valorAlimentos: number;
  valorEquipe: number;
  valorDegustacao: number;
  valorOutros: number;
  valorTotal: number;
  valorPorPessoa: number;
}

const valorMonetario = (rotulo: string) =>
  z
    .number({ message: `O valor ${rotulo} é obrigatório.` })
    .nonnegative(`O valor ${rotulo} não pode ser negativo.`)
    .refine((v) => Math.round(v * 100) / 100 === v, `O valor ${rotulo} deve ter no máximo 2 casas decimais.`);

/**
 * Espelha CreateOrcamentoRequest.java (@NotBlank, @Size, @NotNull, @Positive, @PositiveOrZero, @Digits).
 */
export const CreateOrcamentoRequestSchema = z.object({
  nomeCliente: z
    .string()
    .trim()
    .min(1, "O nome do cliente é obrigatório.")
    .max(255, "O nome do cliente deve ter no máximo 255 caracteres."),
  contatoCliente: z.string().trim().max(255, "O contato do cliente deve ter no máximo 255 caracteres."),
  tipoServico: z.string().trim().max(255, "O tipo de serviço deve ter no máximo 255 caracteres."),
  duracaoEvento: z
    .number({ message: "A duração do evento é obrigatória." })
    .nonnegative("A duração do evento não pode ser negativa."),
  numeroConvidados: z
    .number({ message: "O número de convidados é obrigatório." })
    .int("O número de convidados deve ser um número inteiro.")
    .positive("O número de convidados deve ser maior que zero."),
  valorAlimentos: valorMonetario("de alimentos"),
  valorEquipe: valorMonetario("da equipe"),
  valorDegustacao: valorMonetario("da degustação"),
  valorOutros: valorMonetario("de outros custos"),
  valorTotal: valorMonetario("total"),
  valorPorPessoa: valorMonetario("por pessoa"),
});
export type CreateOrcamentoRequest = z.infer<typeof CreateOrcamentoRequestSchema>;

/**
 * Orçamento da lista "Meus orçamentos": o registro do backend + a composição completa
 * (cardápio, equipe, margens...) guardada neste navegador ao salvar. `orcamento` é null
 * quando ele foi salvo em outro navegador — aí só os totais do backend estão disponíveis.
 */
export interface OrcamentoSalvo extends OrcamentoDetalhadoDTO {
  orcamento: Orcamento | null;
}
