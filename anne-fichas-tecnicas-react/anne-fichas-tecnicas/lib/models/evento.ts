import type { LancamentoListaItemDTO } from "./caixa";
import { z } from "zod";
import {
  CONTAS,
  ETAPAS_EVENTO,
  FUNCOES_EQUIPE,
  type CustoTipo,
  type EtapaEvento,
  type FuncaoEquipe,
  FORMAS_RECEBIMENTO,
  MODOS_ENVIO_EXTRA,
  PRACAS,
  SECOES_ENVIO_EXTRA,
  STATUS_EVENTO,
  STATUS_ITEM_CARDAPIO,
  TIPOS_EVENTO,
  UNIDADES_ENVIO_EXTRA,
  chaves,
  type FormaRecebimento,
  type ModoEnvioExtra,
  type Praca,
  type SecaoEnvioExtra,
  type StatusEvento,
  type StatusItemCardapio,
  type TipoEvento,
} from "./dominio";
import { dataIso, dataIsoOuVazia, dinheiro, dinheiroPositivo, idPositivo, texto } from "./comum";

/* ---------------------------------- Evento --------------------------------- */

export interface ContratoDTO {
  numero: string;
  dataAssinatura: string;
  convidadosContratados: number;
  valorPorConvidado: number;
  valorTotal: number;
  valorConvidadoExtra: number;
  categoriaId: number;
}

/** Item de serviço, prataria ou descartável que a gestão adiciona ao checklist de envio. */
export interface EnvioExtraDTO {
  k: string;
  sec: SecaoEnvioExtra;
  n: string;
  u: string;
  modo: ModoEnvioExtra;
  v: number;
  obs?: string;
}

/** Tarefa do plano da cozinha montado pela gestão (substitui as tarefas fixas). */
export interface PlanoCozinhaItemDTO {
  k?: string;
  desc: string;
  praca?: string;
  dn?: number | null;
  produtos?: string;
  resp?: string;
}

export interface EventoResponseDTO {
  id: number;
  codigo: string; // "AA-NNN"
  nome: string;
  clienteId: number;
  tipo: TipoEvento;
  status: StatusEvento;
  dataEvento: string; // "" quando a combinar
  horaInicio: string;
  local: string;
  cidade: string;
  convidadosPrevistos: number | null;
  margem: number; // % sobre os convidados
  observacoes: string;
  contrato: ContratoDTO | null;
  cardapioFechadoEm: string | null;
  envioExtras: EnvioExtraDTO[];
  planoCozinha: PlanoCozinhaItemDTO[] | null;
  equipeCozinha: string;
  criadoEm: string;
}

export const CreateEventoRequestSchema = z.object({
  nome: texto(200).min(1, "Informe o nome do evento."),
  // Ou um cliente existente, ou um novo cliente criado junto com o evento.
  clienteId: z.number().int().positive().nullable(),
  novoCliente: z.object({ nome: texto(200).min(1, "Informe o nome do novo cliente."), telefone: texto(40).default("") }).nullable().default(null),
  tipo: z.enum(chaves(TIPOS_EVENTO)),
  status: z.enum(chaves(STATUS_EVENTO)),
  dataEvento: dataIsoOuVazia(),
  horaInicio: z.string().trim().max(5).default(""),
  local: texto(200).default(""),
  cidade: texto(100).default(""),
  convidadosPrevistos: z.number().int().positive().nullable(),
  margem: z.number().min(0, "A margem não pode ser negativa.").max(100, "Use uma margem entre 0 e 100%."),
  observacoes: texto(4000).default(""),
}).refine((v) => v.clienteId != null || v.novoCliente != null, { message: "Escolha o cliente ou cadastre um novo.", path: ["clienteId"] });
export type CreateEventoRequestDTO = z.infer<typeof CreateEventoRequestSchema>;

export const UpdateEventoRequestSchema = z.object({
  id: idPositivo("O id do evento não pode ser nulo."),
  nome: texto(200).min(1, "Informe o nome do evento."),
  clienteId: z.number().int().positive().nullable(),
  novoCliente: z.object({ nome: texto(200).min(1, "Informe o nome do novo cliente."), telefone: texto(40).default("") }).nullable().default(null),
  tipo: z.enum(chaves(TIPOS_EVENTO)),
  status: z.enum(chaves(STATUS_EVENTO)),
  dataEvento: dataIsoOuVazia(),
  horaInicio: z.string().trim().max(5).default(""),
  local: texto(200).default(""),
  cidade: texto(100).default(""),
  convidadosPrevistos: z.number().int().positive().nullable(),
  margem: z.number().min(0).max(100, "Use uma margem entre 0 e 100%."),
  observacoes: texto(4000).default(""),
}).refine((v) => v.clienteId != null || v.novoCliente != null, { message: "Escolha o cliente ou cadastre um novo.", path: ["clienteId"] });
export type UpdateEventoRequestDTO = z.infer<typeof UpdateEventoRequestSchema>;

/** Campos que mudam sozinhos, fora do formulário completo (etapa, margem, cardápio fechado, envio extras). */
export const PatchEventoRequestSchema = z.object({
  status: z.enum(chaves(STATUS_EVENTO)).optional(),
  margem: z.number().min(0).max(100, "Use uma margem entre 0 e 100%.").optional(),
  cardapioFechado: z.boolean().optional(),
  envioExtras: z
    .array(
      z.object({
        k: z.string().default(""),
        sec: z.enum(chaves(SECOES_ENVIO_EXTRA)),
        n: texto(200).min(1),
        u: z.enum(UNIDADES_ENVIO_EXTRA),
        modo: z.enum(chaves(MODOS_ENVIO_EXTRA)),
        v: z.number().min(0),
        obs: texto(300).optional(),
      })
    )
    .optional(),
});
export type PatchEventoRequestDTO = z.infer<typeof PatchEventoRequestSchema>;

/* --------------------------------- Contrato -------------------------------- */

export const RegistrarContratoRequestSchema = z.object({
  numero: texto(40).default(""),
  dataAssinatura: dataIso("Informe a data de assinatura."),
  convidadosContratados: z.number().int().positive("Informe os convidados contratados."),
  valorPorConvidado: dinheiro(),
  valorTotal: dinheiroPositivo("Informe o valor total."),
  valorConvidadoExtra: dinheiro(),
  categoriaId: idPositivo("Crie uma categoria de receita em Caixa › Categorias."),
  // Só na criação: geração das parcelas.
  nParcelas: z.number().int().min(1, "Informe ao menos uma parcela.").max(48),
  primeiroVencimento: dataIso("Informe o primeiro vencimento."),
  intervaloMeses: z.union([z.literal(0), z.literal(1), z.literal(2)]),
});
export type RegistrarContratoRequestDTO = z.infer<typeof RegistrarContratoRequestSchema>;

export const AtualizarContratoRequestSchema = RegistrarContratoRequestSchema.omit({
  nParcelas: true,
  primeiroVencimento: true,
  intervaloMeses: true,
});
export type AtualizarContratoRequestDTO = z.infer<typeof AtualizarContratoRequestSchema>;

/* --------------------------------- Parcela --------------------------------- */

export interface ParcelaResponseDTO {
  id: number;
  eventoId: number;
  numero: number;
  vencimento: string;
  valor: number;
  pagoEm: string | null;
  forma: FormaRecebimento | "";
  lancamentoId: number | null;
  /** O que já entrou pelos recebimentos (inclusive em partes) do lançamento da parcela. */
  recebido: number;
}

export const UpdateParcelaRequestSchema = z.object({
  id: idPositivo(),
  vencimento: dataIso("Informe o vencimento."),
  valor: dinheiroPositivo("Informe o valor."),
  pagoEm: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).nullable(),
  forma: z.enum([...chaves(FORMAS_RECEBIMENTO), ""]).default(""),
  conta: z.enum(chaves(CONTAS)).default("banco"),
});
export type UpdateParcelaRequestDTO = z.infer<typeof UpdateParcelaRequestSchema>;

/* ---------------------------------- Extra ---------------------------------- */

export interface ExtraResponseDTO {
  id: number;
  eventoId: number;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  lancamentoId: number | null;
}

export const CreateExtraRequestSchema = z.object({
  eventoId: idPositivo(),
  descricao: texto(200).min(1, "Descreva o extra."),
  quantidade: z.number().positive("Informe quantidade e valor."),
  valorUnitario: dinheiroPositivo("Informe quantidade e valor."),
});
export type CreateExtraRequestDTO = z.infer<typeof CreateExtraRequestSchema>;
export const UpdateExtraRequestSchema = CreateExtraRequestSchema.extend({ id: idPositivo() });
export type UpdateExtraRequestDTO = z.infer<typeof UpdateExtraRequestSchema>;

/* --------------------------------- Cardápio -------------------------------- */

export interface ItemCardapioResponseDTO {
  id: number;
  eventoId: number;
  preparacaoId: number;
  porcao: number;
  status: StatusItemCardapio;
  praca: Praca | null;
  responsavel: string;
}

export const CreateItemCardapioRequestSchema = z.object({
  eventoId: idPositivo(),
  preparacaoId: idPositivo("Escolha a preparação."),
  porcao: z.number().positive("Informe a porção por convidado."),
  praca: z.enum(PRACAS).nullable().default(null),
  responsavel: texto(100).default(""),
});
export type CreateItemCardapioRequestDTO = z.infer<typeof CreateItemCardapioRequestSchema>;
export const UpdateItemCardapioRequestSchema = CreateItemCardapioRequestSchema.extend({ id: idPositivo() });
export type UpdateItemCardapioRequestDTO = z.infer<typeof UpdateItemCardapioRequestSchema>;

export const PatchItemCardapioRequestSchema = z.object({
  status: z.enum(chaves(STATUS_ITEM_CARDAPIO)).optional(),
  porcao: z.number().positive("O fator por convidado precisa ser maior que zero.").optional(),
});
export type PatchItemCardapioRequestDTO = z.infer<typeof PatchItemCardapioRequestSchema>;

/**
 * Adicionar vários pratos de uma vez (protótipo: cardapioMultiForm). O fator por convidado e a praça
 * vêm da ficha; pratos que já estão no cardápio são ignorados.
 */
export const AdicionarPratosCardapioRequestSchema = z.object({
  preparacaoIds: z.array(idPositivo()).min(1, "Marque ao menos um prato."),
});
export type AdicionarPratosCardapioRequestDTO = z.infer<typeof AdicionarPratosCardapioRequestSchema>;

export const CopiarCardapioRequestSchema = z.object({ origemEventoId: idPositivo("Escolha o evento de origem.") });
export type CopiarCardapioRequestDTO = z.infer<typeof CopiarCardapioRequestSchema>;

/** Linhas conferidas da importação por texto (ver /api/eventos/[id]/cardapio/importar). */
export const ImportarCardapioRequestSchema = z.object({
  linhas: z.array(
    z.object({
      texto: z.string(),
      preparacaoId: z.number().int().positive().nullable(),
      // Quando a linha não casou com nenhuma ficha, cria uma ficha incompleta nesta categoria.
      criarCategoria: z.string().nullable().default(null),
    })
  ),
});
export type ImportarCardapioRequestDTO = z.infer<typeof ImportarCardapioRequestSchema>;

export interface ImportarCardapioSugestaoDTO {
  preparacaoId: number;
  nome: string;
  score: number;
  fichaStatus: "Completa" | "Sem preço" | "Incompleta";
}
export interface ImportarCardapioLinhaDTO {
  texto: string;
  situacao: "ok" | "parecido" | "nao" | "ja";
  sugestoes: ImportarCardapioSugestaoDTO[];
}
export interface ImportarCardapioAnaliseResponseDTO {
  cabecalho: string | null;
  linhas: ImportarCardapioLinhaDTO[];
}

/* ------------------------------ Telas de evento ------------------------------ */

export const FILTROS_EVENTO = [
  ["ativos", "Em andamento"],
  ["funil", "Funil"],
  ["contratados", "Contratados"],
  ["realizados", "Realizados"],
  ["arquivo", "Perdidos e cancelados"],
  ["todos", "Todos"],
] as const;
export type FiltroEvento = (typeof FILTROS_EVENTO)[number][0];

export interface EventoListaItemDTO extends EventoResponseDTO {
  codigo: string;
  clienteNome: string | null;
}
export interface EventosResponseDTO {
  ativos: number;
  total: number;
  eventos: EventoListaItemDTO[];
}

export interface CardapioLinhaDTO {
  item: ItemCardapioResponseDTO;
  preparacaoNome: string;
  categoria: string;
  categoriaLabel: string;
  pracaEfetiva: string;
  status: StatusItemCardapio;
  total: number;
  /** "a produzir 150 un" pronto para exibir. */
  aProduzir: string;
  fatorUnid: string;
  fichaStatus: "Completa" | "Sem preço" | "Incompleta";
  custo: number;
  custoDef: number;
  porcaoPadrao: number;
  rendimentoLabel: string;
}

export interface CardapioEventoDTO {
  N: number;
  base: number;
  margem: number;
  fechado: boolean;
  linhas: CardapioLinhaDTO[];
  custo: number;
  custoDef: number;
  custoPorPessoa: number | null;
  pctContrato: number | null;
  abLancado: number;
  nDefinir: number;
  semFicha: string[];
  faltandoPreco: boolean;
}


export interface EventoDetalheResponseDTO {
  evento: EventoResponseDTO;
  codigo: string;
  cliente: { id: number; nome: string; contato: string } | null;
  prazoCardapio: string | null;
  diasPrazo: number | null;
  resultado: { rec: number; desp: number; res: number; mg: number | null };
  parcelas: ParcelaResponseDTO[];
  recebido: number;
  totalParcelas: number;
  extras: ExtraResponseDTO[];
  /** Lançamentos do evento que não são parcela nem extra, com unidade, pago e saldo. */
  outros: LancamentoListaItemDTO[];
  cardapio: CardapioEventoDTO;
  travado: string;
  contratoTemParcelaPaga: boolean;
  categoriaReceitaPadrao: number | null;
  /** As 14 etapas do evento, na ordem do processo (detectadas pelo sistema ou marcadas à mão). */
  etapas: EtapaEventoDTO[];
  /** Escala de equipe do evento (pessoas do cadastro da equipe, reaproveitadas entre eventos). */
  equipe: PessoaEquipeDTO[];
  /** Indicadores e previsto × realizado do evento, calculados no backend. */
  indicadores: IndicadoresEventoDTO;
  /** Tipos de custo com realizado mais de 10% acima do previsto (ou realizado sem previsto). */
  desvios: DesvioCustoDTO[];
  /**
   * Evento como projeto (logística, orçamento, assinaturas, fechamento).
   * Opcional até o backend devolver o bloco: sem ele a ficha mostra só o que já existia.
   */
  projeto?: ProjetoEventoDTO | null;
}

/* ---------------------------- Evento como projeto ---------------------------- */

export type ValoresPorCustoDTO = Record<CustoTipo, number>;

/** Indicadores do evento, calculados no backend (`evProjeto` do protótipo). */
export interface IndicadoresEventoDTO {
  /** Receitas do evento (parcelas + extras), canceladas fora. */
  contratado: number;
  /** contratado, ou o valor do contrato enquanto não há receita lançada. */
  receitaPrevista: number;
  /** contratado passou do valor do contrato (há extras). */
  comExtras: boolean;
  recebido: number;
  aReceber: number;
  previsto: ValoresPorCustoDTO;
  realizado: ValoresPorCustoDTO;
  totalPrevisto: number;
  totalRealizado: number;
  /** Convidados contratados, ou previstos sem contrato. */
  convidados: number;
  /** Saída líquida do estoque geral para o evento (baixas, perdas e sobras − compras por NF do evento), já somada ao A&B realizado. */
  estoqueGeral: number;
  /** Avarias de louça no evento, já somadas a "outros". */
  avarias: number;
  aPagar: number;
  contasAPagar: number;
  resultadoPrevisto: number;
  resultadoRealizado: number;
  cmvPrevisto: number | null;
  cmvRealizado: number | null;
}

/** Tipo de custo com realizado acima de 10% do previsto (desvio null = sem previsto). */
export interface DesvioCustoDTO {
  tipo: CustoTipo;
  previsto: number;
  realizado: number;
  desvio: number | null;
}

export interface EtapaEventoDTO {
  etapa: EtapaEvento;
  feito: boolean;
  /** Detectada pelo sistema (não dá para desmarcar). */
  auto: boolean;
  /** Marcada à mão. */
  manual: boolean;
  dispensada: boolean;
  /** Texto curto de situação ("2 sem ficha", "carga assinada por …"). */
  info: string;
  /** Quando foi marcada à mão (ISO). */
  em: string | null;
  /** Etapa crítica ainda pendente a poucos dias do evento. */
  atrasada: boolean;
  /** Dias até o evento (para o aviso "pendente a N dias"). */
  diasParaEvento: number | null;
}

export interface PessoaEquipeDTO {
  /** Linha da escala: usado para editar e tirar da escala. */
  id: number;
  /** Pessoa no cadastro da equipe (a mesma em todos os eventos em que for escalada). */
  equipeId: number;
  nome: string;
  funcao: FuncaoEquipe;
  chegada: string;
  saida: string;
  valor: number;
  contato: string;
  pix: string;
  /** Conta a pagar da diária, quando já lançada. */
  lancamentoId: number | null;
  lancamentoStatus: "previsto" | "realizado" | null;
  /** Diária já paga (no todo ou em parte): valor não pode mudar e a pessoa não sai da escala. */
  pago: boolean;
}

export interface LogisticaEventoDTO {
  veiculos: string;
  motorista: string;
  viagens: string;
  saida: string;
  montagem: string;
  retorno: string;
  obs: string;
  atualizadoEm: string | null;
}

export interface OrcamentoEventoDTO {
  /** Usado só enquanto não há escala montada. */
  equipe: number | null;
  transporte: number;
  outros: number;
  atualizadoEm: string | null;
}

/** Assinaturas que a cozinha deixou no checklist (carga, recebimento no evento e retorno). */
export interface AssinaturaCozinhaDTO {
  por: string;
  hora: string;
}

/** Retrato guardado ao fechar o evento. */
export interface FechamentoEventoDTO {
  em: string;
  por: string;
  receita: number;
  recebido: number;
  custoPrevisto: number;
  custoRealizado: number;
  resultado: number;
  cmv: number | null;
  porTipo: { previsto: ValoresPorCustoDTO; realizado: ValoresPorCustoDTO };
  pendencias: EtapaEvento[];
}

export interface ProjetoEventoDTO {
  logistica: LogisticaEventoDTO;
  orcamento: OrcamentoEventoDTO;
  assinaturas: { carga: AssinaturaCozinhaDTO | null; recebimento: AssinaturaCozinhaDTO | null; retorno: AssinaturaCozinhaDTO | null };
  fechamento: FechamentoEventoDTO | null;
  /** O que conferir antes de fechar (vazio = tudo conferido). */
  pendenciasFechamento: string[];
}

const HORA_RE = /^(\d{2}:\d{2})?$/;
const hora = () => z.string().trim().regex(HORA_RE, "Horário inválido.").default("");

/**
 * Pessoa na escala. Na criação, equipeId escolhe alguém do cadastro da equipe; sem ele, o backend
 * procura pelo nome (sem acentos/maiúsculas) e só cadastra pessoa nova se não achar. Na edição é ignorado.
 */
export const SalvarPessoaEquipeRequestSchema = z.object({
  equipeId: z.number().int().positive().optional(),
  nome: texto(120).min(1, "Informe o nome."),
  funcao: z.enum(FUNCOES_EQUIPE),
  chegada: hora(),
  saida: hora(),
  valor: dinheiro(),
  contato: texto(40).default(""),
  pix: texto(120).default(""),
});
export type SalvarPessoaEquipeRequestDTO = z.infer<typeof SalvarPessoaEquipeRequestSchema>;

/** Checklist da escala: a lista é a escala inteira (entra quem não estava, sai quem foi desmarcado). */
export const AssociarEquipeRequestSchema = z.object({
  equipeIds: z.array(idPositivo()).max(500, "A escala aceita até 500 pessoas por vez."),
});
export type AssociarEquipeRequestDTO = z.infer<typeof AssociarEquipeRequestSchema>;

export interface AssociarEquipeResponseDTO {
  adicionados: number;
  removidos: number;
}

export interface LancarEquipeResponseDTO {
  lancados: number;
  vencimento: string;
}

export const SalvarLogisticaRequestSchema = z.object({
  veiculos: texto(200).default(""),
  motorista: texto(120).default(""),
  viagens: texto(120).default(""),
  saida: hora(),
  montagem: hora(),
  retorno: hora(),
  obs: texto(2000).default(""),
});
export type SalvarLogisticaRequestDTO = z.infer<typeof SalvarLogisticaRequestSchema>;

export const SalvarOrcamentoRequestSchema = z.object({
  /** null quando a equipe vem da escala. */
  equipe: dinheiro().nullable(),
  transporte: dinheiro(),
  outros: dinheiro(),
});
export type SalvarOrcamentoRequestDTO = z.infer<typeof SalvarOrcamentoRequestSchema>;

/** Marcar/desmarcar uma etapa manual ou dispensar/incluir a degustação. */
export const AjustarEtapaRequestSchema = z
  .object({
    etapa: z.enum(chaves(ETAPAS_EVENTO)),
    feito: z.boolean().optional(),
    dispensada: z.boolean().optional(),
  })
  .refine((v) => v.feito !== undefined || v.dispensada !== undefined, "Nada a ajustar.");
export type AjustarEtapaRequestDTO = z.infer<typeof AjustarEtapaRequestSchema>;

/** Formulários do módulo de eventos que pedem opções; cada um recebe só os campos que usa. */
export const FORMS_OPCOES_EVENTO = ["evento", "contrato", "lancamento", "cardapio", "copiar-cardapio"] as const;
export type FormOpcoesEvento = (typeof FORMS_OPCOES_EVENTO)[number];

/** Todos os campos de opções do módulo de eventos; ver `OpcoesEventoDe` para o recorte de cada formulário. */
export interface EventoOpcoesResponseDTO {
  clientes: [number, string][];
  categoriasReceita: [number, string][];
  categoriasTodas: { grupo: string; itens: [number, string][] }[];
  preparacoes: { grupo: string; itens: [number, string][] }[];
  /** temIngredientes: a ficha tem ao menos um insumo ou sub-receita (opcional até o backend devolver). */
  preparacoesInfo: Record<number, { porcaoPadrao: number; praca: string; unidadeRendimento: string; rendimento: number; temIngredientes?: boolean }>;
  eventos: [number, string][];
  /** Data de cada evento, para a competência sugerida no lançamento. */
  eventosData: Record<number, string>;
  eventosComCardapio: { id: number; rotulo: string }[];
}

/** Campos que o backend devolve para cada formulário (`?para=`). */
export interface OpcoesEventoDe {
  evento: Pick<EventoOpcoesResponseDTO, "clientes">;
  contrato: Pick<EventoOpcoesResponseDTO, "categoriasReceita">;
  lancamento: Pick<EventoOpcoesResponseDTO, "categoriasTodas" | "eventos" | "eventosData">;
  cardapio: Pick<EventoOpcoesResponseDTO, "preparacoes" | "preparacoesInfo">;
  "copiar-cardapio": Pick<EventoOpcoesResponseDTO, "eventosComCardapio">;
}
