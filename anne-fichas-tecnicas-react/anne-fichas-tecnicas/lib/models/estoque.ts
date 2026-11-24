import { z } from "zod";
import {
  DESTINOS_LOTE,
  FORMAS_PAGAMENTO_NF,
  MOTIVOS_DESCARTE_LOTE,
  MOVIMENTOS_MANUAIS,
  USOS_LOTE,
  chaves,
  type DestinoLote,
  type FormaPagamentoNf,
  type OrigemMovimento,
  type ParaLote,
  type StatusNf,
  type TipoMovimento,
  UNIDADES,
  UN_OPER,
  type HorizonteCompra,
  type Unidade,
  type UnidadeOperacional,
} from "./dominio";
import { dataIso, dinheiro, idPositivo, texto } from "./comum";
import type { InsumoResponseDTO } from "./insumo";

/* -------------------------------- Movimento -------------------------------- */

export interface MovimentoResponseDTO {
  id: number;
  tipo: TipoMovimento;
  insumoId: number;
  /** Efeito no saldo, com sinal. */
  qtd: number;
  qtdInformada: number;
  unidade: string;
  data: string;
  motivo: string;
  eventoId: number | null;
  custoUnit: number | null;
  origem: OrigemMovimento;
  nfId: number | null;
  criadoEm: string;
  /** Baixa calculada (checklist, ordem de produção ou lote), não gravada. */
  virtual?: boolean;
  /** Validade informada na entrada (perecíveis); nula nos demais tipos. */
  validade: string | null;
  /** Consumo: unidade de negócio que usou o item; nula nos demais tipos. */
  unidadeUso: UnidadeOperacional | null;
  /** Fornecedor da entrada (da NF, quando houver). */
  fornecedor?: string;
}

export const CreateMovimentoRequestSchema = z
  .object({
    tipo: z.enum(MOVIMENTOS_MANUAIS),
    insumoId: idPositivo("Escolha o item."),
    data: dataIso(),
    /** Quantidade informada; em ajuste é a contagem real. */
    qtd: z.number().min(0),
    custoUnit: dinheiro().nullable().default(null),
    eventoId: z.number().int().positive().nullable().default(null),
    motivo: texto(300).default(""),
    /** Entrada: validade do lote recebido (aparece em Estoque › Validades). */
    validade: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Validade inválida.").nullable().default(null),
    /** Consumo: unidade que usou. */
    unidadeUso: z.enum(UN_OPER).nullable().default(null),
    /** Consumo: unidade que pagou a compra; se diferente de quem usou, o custo vira transferência. */
    compradoPor: z.enum(chaves(UNIDADES)).nullable().default(null),
  })
  .refine((v) => v.tipo !== "consumo" || v.unidadeUso != null, { message: "Escolha a unidade que usou.", path: ["unidadeUso"] })
  .refine((v) => !v.compradoPor || v.compradoPor !== v.unidadeUso, { message: 'Quem comprou e quem usou são a mesma unidade: deixe "A mesma unidade".', path: ["compradoPor"] });
export type CreateMovimentoRequestDTO = z.infer<typeof CreateMovimentoRequestSchema>;

export const InventarioRequestSchema = z.object({ contagens: z.array(z.object({ insumoId: idPositivo(), contado: z.number().min(0) })).min(1, "Nenhum item com diferença.") });
export type InventarioRequestDTO = z.infer<typeof InventarioRequestSchema>;

export type SituacaoTom = "" | "good" | "warn" | "bad";

export interface SaldoItemDTO {
  insumo: InsumoResponseDTO;
  fisico: number;
  reservado: number;
  disponivel: number;
  valor: number;
  situacao: [SituacaoTom, string];
  emFichas: number;
  reservas: { eventoId: number; codigo: string; nome: string; qtd: number }[];
  /** Só louças e utensílios. */
  fora: number;
}

export interface EstoqueSaldoResponseDTO {
  kpis: { valor: number; semPreco: number; comSaldo: number; consumiveis: number; abaixo: number; negativos: number };
  itens: SaldoItemDTO[];
  semMovimentos: boolean;
}

export interface MovimentoListaItemDTO extends MovimentoResponseDTO {
  insumoNome: string;
  eventoNome: string | null;
}

export interface LoucaDisponibilidadeDTO {
  insumo: InsumoResponseDTO;
  total: number;
  reservado: number;
  reservas: { codigo: string; qtd: number }[];
  fora: number;
  disponivel: number;
}
export interface EstoqueLoucasResponseDTO {
  data: string;
  eventosNaData: { id: number; codigo: string; nome: string }[];
  itens: LoucaDisponibilidadeDTO[];
}

/* ------------------------------- Nota fiscal ------------------------------- */

export interface ItemNfDTO {
  desc: string;
  cod: string;
  uCom: string;
  insumoId: number | null;
  qtdNota: number;
  unid: "estoque" | "compra";
  /** Quantidade na unidade de estoque. */
  qtd: number;
  valorUnit: number | null;
  valor: number;
  conferido: boolean;
}
export interface ParcelaNfDTO {
  venc: string;
  valor: number;
  pago: boolean;
}

export interface NotaFiscalResponseDTO {
  id: number;
  fornecedor: string;
  numero: string;
  emissao: string;
  eventoId: number | null;
  lancarCaixa: boolean;
  categoriaId: number | null;
  formaPag: FormaPagamentoNf;
  parcelas: ParcelaNfDTO[];
  pago: boolean;
  status: StatusNf;
  itens: ItemNfDTO[];
  acrescimo: number;
  total: number;
  chave: string;
  serie: string;
  razaoSocial: string;
  cnpjFornecedor: string;
  importadaXML: boolean;
  importadaFoto: boolean;
  movIds: number[];
  lancIds: number[];
  lancadaEm: string | null;
  criadoEm: string;
  unidade: Unidade | null;
}

const ItemNfSchema = z.object({
  desc: texto(200).default(""),
  cod: texto(60).default(""),
  uCom: texto(10).default(""),
  insumoId: z.number().int().positive().nullable().default(null),
  qtdNota: z.number().min(0).default(0),
  unid: z.enum(["estoque", "compra"]).default("estoque"),
  valorUnit: z.number().min(0).nullable().default(null),
  valor: dinheiro().default(0),
  conferido: z.boolean().default(false),
});

export const SalvarNotaFiscalRequestSchema = z.object({
  fornecedor: texto(120).min(1, "Informe o fornecedor."),
  numero: texto(20).default(""),
  emissao: dataIso("Informe a data de emissão."),
  eventoId: z.number().int().positive().nullable().default(null),
  lancarCaixa: z.boolean().default(true),
  categoriaId: z.number().int().positive().nullable().default(null),
  formaPag: z.enum(chaves(FORMAS_PAGAMENTO_NF)).default("boleto"),
  parcelas: z.array(z.object({ venc: z.string().default(""), valor: z.number().min(0).default(0), pago: z.boolean().default(false) })).default([]),
  itens: z.array(ItemNfSchema).default([]),
  acrescimo: z.number().default(0),
  chave: z.string().default(""),
  serie: z.string().default(""),
  razaoSocial: z.string().default(""),
  cnpjFornecedor: z.string().default(""),
  importadaXML: z.boolean().default(false),
  importadaFoto: z.boolean().default(false),
  /** Unidade de negócio que paga e usa a compra; obrigatória para lançar no caixa. */
  unidade: z.union([z.enum(chaves(UNIDADES)), z.literal("")]).default(""),
  /** Dar entrada mesmo com a soma das parcelas diferente do total: a diferença vira acréscimo ou desconto. */
  aceitarDiferenca: z.boolean().default(false),
});
export type SalvarNotaFiscalRequestDTO = z.infer<typeof SalvarNotaFiscalRequestSchema>;

/** Dados lidos de um XML de NF-e (ou de uma foto), para pré-preencher a nota. */
export interface NotaFiscalImportadaDTO {
  chave: string;
  numero: string;
  serie: string;
  emissao: string;
  fornecedor: string;
  razaoSocial: string;
  cnpjFornecedor: string;
  itens: ItemNfDTO[];
  total: number;
  acrescimo: number;
  formaPag: FormaPagamentoNf;
  parcelas: ParcelaNfDTO[];
  importadaXML: boolean;
  importadaFoto: boolean;
  duvidas: string[];
  /** Id da nota já existente com a mesma chave, se houver. */
  duplicadaDe: number | null;
}

export interface NotaFiscalListaItemDTO extends NotaFiscalResponseDTO {
  eventoCodigo: string | null;
  eventoNome: string | null;
  proximoVencimento: string | null;
  quitada: boolean;
  /** Parcelas no caixa (só nota lançada). */
  lancamentos: ParcelaNfLancadaDTO[];
}

/* ----------------------------------- Lote ---------------------------------- */

export interface HistoricoLoteDTO {
  tipo: "uso" | "descarte";
  data: string;
  qtd: number;
  para: ParaLote;
  /** Uso em evento: qual evento (a ordem e o checklist dele deixam de baixar essa quantidade). */
  eventoId: number | null;
  obs: string;
  em: string;
}

export interface LoteResponseDTO {
  id: number;
  codigo: string;
  preparacaoId: number;
  qtd: number;
  saldo: number;
  unidade: string;
  produzidoEm: string;
  validade: string;
  destino: DestinoLote;
  eventoId: number | null;
  baixaInsumos: boolean;
  responsavel: string;
  obs: string;
  historico: HistoricoLoteDTO[];
  criadoEm: string;
}

export const CreateLoteRequestSchema = z
  .object({
    preparacaoId: idPositivo("Escolha a preparação."),
    qtd: z.number().positive("Informe a quantidade produzida."),
    produzidoEm: dataIso("Informe a data de produção."),
    validade: z.string().default(""),
    destino: z.enum(chaves(DESTINOS_LOTE)).default("restaurante"),
    eventoId: z.number().int().positive().nullable().default(null),
    responsavel: texto(100).default(""),
    obs: texto(1000).default(""),
  })
  .refine((v) => !v.validade || v.validade >= v.produzidoEm, { message: "A validade não pode ser antes da produção.", path: ["validade"] })
  .refine((v) => v.destino !== "evento" || v.eventoId != null, { message: "Escolha o evento.", path: ["eventoId"] });
export type CreateLoteRequestDTO = z.infer<typeof CreateLoteRequestSchema>;

/** Lote com uso já registrado: só validade, responsável e observações. */
export const UpdateLoteRequestSchema = z.object({ id: idPositivo(), validade: z.string().default(""), responsavel: texto(100).default(""), obs: texto(1000).default("") });
export type UpdateLoteRequestDTO = z.infer<typeof UpdateLoteRequestSchema>;

export const MovimentarLoteRequestSchema = z
  .object({
    tipo: z.enum(["uso", "descarte"]),
    qtd: z.number().positive("Informe a quantidade."),
    data: dataIso(),
    para: z.enum([...chaves(USOS_LOTE), ...chaves(MOTIVOS_DESCARTE_LOTE)]),
    /** Uso em evento: qual evento. A ordem e o checklist dele deixam de baixar os insumos dessa quantidade. */
    eventoId: z.number().int().positive().nullable().default(null),
    obs: texto(300).default(""),
  })
  .refine((v) => !(v.tipo === "uso" && v.para === "evento") || v.eventoId != null, { message: "Escolha o evento.", path: ["eventoId"] });
export type MovimentarLoteRequestDTO = z.infer<typeof MovimentarLoteRequestSchema>;

export type LoteSituacao = [SituacaoTom, "Esgotado" | "Sem validade" | "Vencido" | "Vence hoje" | "Vence amanhã" | "Válido"];

export interface LoteListaItemDTO extends LoteResponseDTO {
  preparacaoNome: string;
  eventoNome: string | null;
  situacao: LoteSituacao;
  valor: number;
  conservacao: string;
}

export interface EstoquePreparosResponseDTO {
  kpis: { comSaldo: number; preparacoes: number; vencemHojeAmanha: number; vencidos: number; valor: number };
  prontoAgora: { preparacaoId: number; nome: string; total: number; unidade: string; lotes: number; usarPrimeiro: LoteListaItemDTO }[];
  lotes: LoteListaItemDTO[];
}

/* ------------------------------ Telas de estoque ------------------------------ */

export interface ItemEstoqueDetalheDTO extends SaldoItemDTO {
  movimentos: MovimentoListaItemDTO[];
  usadoEm: string[];
  /** Histórico de preços e fornecedores (entradas com preço). Opcional até o backend devolver. */
  precos?: HistoricoPrecosDTO | null;
}

export interface HistoricoPrecosDTO {
  /** Data da primeira entrada com preço e variação do primeiro ao último preço. */
  desde: string | null;
  variacao: number | null;
  fornecedores: { fornecedor: string; compras: number; ultimo: number; menor: number }[];
  /** Até 12 entradas, da mais recente para a mais antiga; variação sobre a entrada anterior. */
  entradas: { data: string; fornecedor: string; qtd: number; custoUnit: number; variacao: number | null }[];
}

/* ------------------------------ Compras sugeridas ------------------------------ */

/** Comprar = reservas dos eventos no horizonte + mínimo − saldo físico, arredondado à embalagem. */
export interface SugestaoCompraDTO {
  insumoId: number;
  nome: string;
  unidade: string;
  unidadeCompra: string;
  fornecedor: string;
  saldo: number;
  reservado: number;
  minimo: number;
  falta: number;
  /** Embalagens de compra (0 quando o item não tem fator de compra). */
  embalagens: number;
  /** Quantidade a comprar na unidade de estoque. */
  comprar: number;
  valor: number;
  semPreco: boolean;
  /** Códigos dos eventos que pedem o item. */
  eventos: string[];
  /** Pedido feito e mercadoria ainda não entrou. */
  pedido: boolean;
  pedidoEm: string | null;
}

export interface EstoqueComprasResponseDTO {
  horizonte: HorizonteCompra;
  kpis: { itens: number; fornecedores: number; valor: number; semPreco: boolean; pedidos: number; eventos: number };
  /** Agrupado por fornecedor ("Sem fornecedor definido" por último). */
  fornecedores: { fornecedor: string; valor: number; itens: SugestaoCompraDTO[] }[];
}

export const MarcarPedidoRequestSchema = z.object({ insumoId: idPositivo(), feito: z.boolean() });
export type MarcarPedidoRequestDTO = z.infer<typeof MarcarPedidoRequestSchema>;

/* -------------------------------- Validades -------------------------------- */

export interface ValidadeItemDTO {
  tipo: "insumo" | "lote";
  validade: string;
  /** Dias até vencer (negativo = vencido). */
  dias: number;
  nome: string;
  /** "Lote L251118-01 · 2 kg" ou "3 kg · entrada de 12/11". */
  detalhe: string;
  qtd: number;
  unidade: string;
  valor: number;
  insumoId: number | null;
  loteId: number | null;
}

export interface EstoqueValidadesResponseDTO {
  kpis: { vencidos: number; vencem: number; valorRisco: number; semValidade: number };
  itens: ValidadeItemDTO[];
}

/* ---------------------------------- Perdas --------------------------------- */

export interface PerdaDTO {
  data: string;
  nome: string;
  qtd: number;
  unidade: string;
  valor: number;
  motivo: string;
  grupo: "Perda de insumo" | "Avaria de louça e utensílio" | "Descarte de pré-preparo";
  eventoNome: string | null;
  insumoId: number | null;
  loteId: number | null;
}

export interface EstoquePerdasResponseDTO {
  mes: string;
  kpis: { total: number; registros: number; comprasMes: number; pctCompras: number | null; sobras: number; sobrasRegistros: number; maiorCausa: string | null };
  porGrupo: { grupo: string; valor: number }[];
  perdas: PerdaDTO[];
}

/* -------------------------------- Conferência -------------------------------- */

export interface PontoConferenciaDTO {
  tom: "warn" | "bad";
  texto: string;
  insumoId: number | null;
  loteId: number | null;
  nfId: number | null;
}

export interface EstoqueConferenciaResponseDTO {
  porOrigem: { origem: string; n: number }[];
  pontos: PontoConferenciaDTO[];
}

export interface MovimentosResponseDTO {
  movimentos: MovimentoListaItemDTO[];
  total: number;
}

/** Formulários do estoque que pedem opções; cada um recebe só os campos que usa. */
export const FORMS_OPCOES_ESTOQUE = ["movimento", "nf", "lote", "transferencia"] as const;
export type FormOpcoesEstoque = (typeof FORMS_OPCOES_ESTOQUE)[number];

/** Todos os campos de opções do estoque; ver `OpcoesEstoqueDe` para o recorte de cada formulário. */
export interface EstoqueOpcoesResponseDTO {
  itens: { grupo: string; itens: [number, string][] }[];
  itensInfo: Record<
    number,
    { nome: string; unidade: string; fatorCompra: number | null; unidadeCompra: string; reutilizavel: boolean; saldo: number; local: string; custo: number }
  >;
  eventos: [number, string][];
  /** Eventos ativos para destino de NF ou lote: [id, "código · nome · data"]. */
  eventosAtivos: [number, string][];
  /** Eventos para lote de evento (de 3 dias atrás em diante). */
  eventosLote: [number, string][];
  preparacoes: { grupo: string; itens: [number, string][] }[];
  preparacoesInfo: Record<number, { nome: string; unidade: string; rendimento: number; validadeDias: number; conservacao: string }>;
  categoriasDespesa: { grupo: string; itens: [number, string][] }[];
  categoriaAB: number | null;
  fornecedores: string[];
}

/** Campos que o backend devolve para cada formulário (`?para=`). */
export interface OpcoesEstoqueDe {
  movimento: Pick<EstoqueOpcoesResponseDTO, "itens" | "itensInfo" | "eventos">;
  nf: Pick<EstoqueOpcoesResponseDTO, "itens" | "itensInfo" | "eventosAtivos" | "categoriasDespesa" | "categoriaAB" | "fornecedores">;
  lote: Pick<EstoqueOpcoesResponseDTO, "preparacoes" | "preparacoesInfo" | "eventosLote">;
  transferencia: Pick<EstoqueOpcoesResponseDTO, "itens" | "itensInfo" | "categoriasDespesa" | "categoriaAB">;
}

export const NfDeParaRequestSchema = z.object({ fornecedor: z.string().default(""), itens: z.array(z.object({ desc: z.string().default(""), cod: z.string().default("") })) });
export type NfDeParaRequestDTO = z.infer<typeof NfDeParaRequestSchema>;

/** Parcela de NF já lançada no caixa. */
export interface ParcelaNfLancadaDTO {
  id: number;
  parcelaNF: number | null;
  valor: number;
  status: "previsto" | "realizado" | "cancelado";
  vencimento: string | null;
  dataCompetencia: string;
  dataPagamento: string | null;
  formaPag: FormaPagamentoNf | null;
}

/* ------------------- Estoque integrado: estrutura sempre completa ------------------- */

/**
 * As abas Compras, Validades, Perdas e Conferência mostram todos os blocos mesmo sem dados:
 * campo nulo ou ausente na resposta vira zero, texto vazio ou lista vazia.
 */
type Talvez<T> = T | null | undefined;
const num = (x: Talvez<number>): number => (typeof x === "number" && Number.isFinite(x) ? x : 0);
const txt = (x: Talvez<string>): string => x ?? "";
const lista = <T>(x: Talvez<T[]>): T[] => (Array.isArray(x) ? x : []);

export const GRUPOS_PERDA = ["Perda de insumo", "Avaria de louça e utensílio", "Descarte de pré-preparo"] as const;

export function normalizarCompras(r: Talvez<Partial<EstoqueComprasResponseDTO>>, horizonte: HorizonteCompra): EstoqueComprasResponseDTO {
  const k = r?.kpis;
  return {
    horizonte: r?.horizonte ?? horizonte,
    kpis: { itens: num(k?.itens), fornecedores: num(k?.fornecedores), valor: num(k?.valor), semPreco: !!k?.semPreco, pedidos: num(k?.pedidos), eventos: num(k?.eventos) },
    fornecedores: lista(r?.fornecedores).map((f) => ({
      fornecedor: f.fornecedor || "Sem fornecedor definido",
      valor: num(f.valor),
      itens: lista(f.itens).map((x) => ({
        ...x,
        nome: txt(x.nome),
        unidade: txt(x.unidade),
        unidadeCompra: txt(x.unidadeCompra),
        saldo: num(x.saldo),
        reservado: num(x.reservado),
        minimo: num(x.minimo),
        falta: num(x.falta),
        embalagens: num(x.embalagens),
        comprar: num(x.comprar),
        valor: num(x.valor),
        semPreco: !!x.semPreco,
        eventos: lista(x.eventos),
        pedido: !!x.pedido,
        pedidoEm: x.pedidoEm ?? null,
      })),
    })),
  };
}

export function normalizarValidades(r: Talvez<Partial<EstoqueValidadesResponseDTO>>): EstoqueValidadesResponseDTO {
  const k = r?.kpis;
  return {
    kpis: { vencidos: num(k?.vencidos), vencem: num(k?.vencem), valorRisco: num(k?.valorRisco), semValidade: num(k?.semValidade) },
    itens: lista(r?.itens).map((x) => ({
      ...x,
      dias: num(x.dias),
      nome: txt(x.nome),
      detalhe: txt(x.detalhe),
      qtd: num(x.qtd),
      unidade: txt(x.unidade),
      valor: num(x.valor),
      insumoId: x.insumoId ?? null,
      loteId: x.loteId ?? null,
    })),
  };
}

export function normalizarPerdas(r: Talvez<Partial<EstoquePerdasResponseDTO>>, mes: string): EstoquePerdasResponseDTO {
  const k = r?.kpis;
  return {
    mes: r?.mes || mes,
    kpis: {
      total: num(k?.total),
      registros: num(k?.registros),
      comprasMes: num(k?.comprasMes),
      pctCompras: typeof k?.pctCompras === "number" && Number.isFinite(k.pctCompras) ? k.pctCompras : null,
      sobras: num(k?.sobras),
      sobrasRegistros: num(k?.sobrasRegistros),
      maiorCausa: k?.maiorCausa || null,
    },
    porGrupo: lista(r?.porGrupo).map((g) => ({ grupo: txt(g.grupo), valor: num(g.valor) })),
    perdas: lista(r?.perdas).map((x) => ({
      ...x,
      nome: txt(x.nome),
      qtd: num(x.qtd),
      unidade: txt(x.unidade),
      valor: num(x.valor),
      motivo: txt(x.motivo),
      eventoNome: x.eventoNome || null,
      insumoId: x.insumoId ?? null,
      loteId: x.loteId ?? null,
    })),
  };
}

export function normalizarConferencia(r: Talvez<Partial<EstoqueConferenciaResponseDTO>>): EstoqueConferenciaResponseDTO {
  return {
    porOrigem: lista(r?.porOrigem).map((o) => ({ origem: o.origem || "Origem não informada", n: num(o.n) })),
    pontos: lista(r?.pontos).map((x) => ({ ...x, texto: txt(x.texto), insumoId: x.insumoId ?? null, loteId: x.loteId ?? null, nfId: x.nfId ?? null })),
  };
}
