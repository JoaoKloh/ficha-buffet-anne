import { z } from "zod";
import {
  ITENS_LIBERACAO,
  MOTIVOS_BLOQUEIO,
  TIPOS_OCORRENCIA,
  chaves,
  type CategoriaPreparacao,
  type Conservacao,
  type EtapaOrdem,
  type ItemLiberacao,
  type StatusEvento,
  type StatusItemCardapio,
  type StatusOrdem,
  type TipoOcorrencia,
} from "./dominio";
import { idPositivo, texto } from "./comum";
import type { FichaStatus } from "./preparacao";

/* ------------------------- Execução registrada pela cozinha ------------------------- */

export const KINDS_EXECUCAO = ["item", "comp", "extra", "louca", "tarefa", "compras", "transporte"] as const;
export type KindExecucao = (typeof KINDS_EXECUCAO)[number];

/**
 * Um registro de execução. A chave é composta: evento + tipo + referência
 * (item do cardápio, componente, insumo, tarefa). Os campos preenchidos
 * dependem do tipo, como no checklist de papel.
 */
export interface ExecucaoResponseDTO {
  id: number;
  chave: string; // "i_12", "c_12__c0", "x_3__xabc", "l_3__7", "t_3__f_0", "cp_3", "tr_3"
  kind: KindExecucao;
  eventoId: number;
  cardapioId: number | null;
  insumoId: number | null;
  key: string | null;
  produzido: number | null;
  produzidoEm: string | null;
  executadoPor: string;
  enviadoQ: number | null;
  enviado: boolean;
  enviadoEm: string | null;
  separadoPor: string;
  retorno: number | null;
  conferidoPor: string;
  obs: string;
  tempSaida: number | null;
  tempChegada: number | null;
  saiu: number | null;
  voltou: number | null;
  avaria: number | null;
  feito: boolean;
  resp: string;
  dnSet: boolean;
  dn: number | null;
  itens: Record<string, boolean>;
  chefPor: string;
  chefHora: string;
  cargaPor: string;
  cargaHora: string;
  recebPor: string;
  recebHora: string;
  retornoPor: string;
  retornoHora: string;
  veiculo: string;
  /** Item da lista de produção: horários de início e conclusão ("HH:MM"). */
  inicio?: string;
  fim?: string;
  atualizadoEm: string;
}

/** Gravação parcial: só os campos tocados. O BFF mescla com o registro atual. */
export const SalvarExecucaoRequestSchema = z.object({
  chave: z.string().min(1),
  kind: z.enum(KINDS_EXECUCAO),
  eventoId: z.number().int().positive(),
  cardapioId: z.number().int().positive().nullable().optional(),
  insumoId: z.number().int().positive().nullable().optional(),
  key: z.string().nullable().optional(),
  campos: z.record(z.union([z.string(), z.number(), z.boolean(), z.null(), z.record(z.boolean())])),
});
export type SalvarExecucaoRequestDTO = z.infer<typeof SalvarExecucaoRequestSchema>;

/* --------------------------- Projeção do evento para a cozinha --------------------------- */

export interface InsumoPratoProjecaoDTO {
  k: string;
  n: string;
  u: string;
  via: string;
  cons: Conservacao;
  qb: number; // por receita
  qt: number; // total do evento
}
export interface ComponenteProjecaoDTO {
  k: string;
  n: string;
  u: string;
  q: number;
  prev: number;
  cons: Conservacao;
  obs: string;
}
export interface PratoProjecaoDTO {
  cid: number;
  nome: string;
  cat: CategoriaPreparacao | "";
  praca: string;
  resp: string;
  st: StatusItemCardapio;
  porcao: number;
  fator: string;
  total: number;
  uu: string;
  ulabel: string;
  rend: number;
  rendLabel: string;
  lotes: number;
  conserv: Conservacao;
  ficha: FichaStatus;
  modo: string;
  obsEnvio: string;
  etapas: { desc: string; praca: string; dn: number }[];
  ins: InsumoPratoProjecaoDTO[];
  loucas: { n: string; q: number }[];
  comp: ComponenteProjecaoDTO[];
  /** Antecedência efetiva da produção (ficha ou padrão da praça), em dias antes do evento. */
  dnf?: number;
  /** Comprado pronto do fornecedor: a produção não faz, só retira do estoque e finaliza. */
  pronto?: boolean;
  /** Fornecedor do item comprado pronto. */
  forn?: string;
}
export interface CompraProjecaoDTO {
  id: number;
  n: string;
  u: string;
  cat: string;
  q: number;
  qd: number;
  est: number;
  fc: number;
  uc: string;
  usos: string;
}
export interface TarefaProjecaoDTO {
  k: string;
  desc: string;
  praca: string;
  dn: number; // -1 = a posicionar
  origem: string;
  produtos: string;
  resp: string;
  padrao: boolean;
}
export interface ExtraProjecaoDTO {
  k: string;
  sec: string;
  n: string;
  u: string;
  q: number;
  obs: string;
}

export interface CozinhaProjecaoDTO {
  eventoId: number;
  codigo: string;
  nome: string;
  data: string;
  hora: string;
  local: string;
  cidade: string;
  status: StatusEvento;
  N: number;
  base: number;
  margem: number;
  fechado: boolean;
  obs: string;
  equipe: string;
  pratos: PratoProjecaoDTO[];
  compras: CompraProjecaoDTO[];
  loucas: { id: number; n: string; q: number; usos: string }[];
  tarefas: TarefaProjecaoDTO[];
  extras: ExtraProjecaoDTO[];
  /** Mapa de produção do evento (null = ainda não gerado). */
  producao?: MapaProducaoDTO | null;
}

/** Tarefa com os ajustes da cozinha (feito, responsável, dia movido) aplicados. */
export interface TarefaCozinhaDTO extends TarefaProjecaoDTO {
  eventoId: number;
  codigo: string;
  eventoNome: string;
  eventoData: string;
  dnEfetivo: number | null;
  data: string | null;
  feito: boolean;
  movida: boolean;
}

export interface CozinhaResponseDTO {
  projecoes: CozinhaProjecaoDTO[];
  execucoes: ExecucaoResponseDTO[];
  tarefas: TarefaCozinhaDTO[];
  /*
   * Ordens de produção dos eventos com mapa gerado, com a liberação para expedição de cada
   * um e os setores da cozinha. No modo cozinha a tela filtra as ordens de mapas ainda não
   * liberados para a equipe. Ocorrências e indicadores ainda não vêm do backend.
   */
  ordens?: OrdemProducaoDTO[];
  liberacoes?: LiberacaoExpedicaoDTO[];
  ocorrencias?: OcorrenciaDTO[];
  setores?: SetorProducaoDTO[];
  /** Só na gestão: indicadores com custo. Na cozinha, sem as colunas de custo. */
  indicadoresProducao?: IndicadoresProducaoDTO | null;
}

export interface SalvarExecucaoResponseDTO {
  execucao: ExecucaoResponseDTO;
  /** A quantidade enviada foi apagada porque a linha deixou de cumprir as condições de saída. */
  envioApagado: boolean;
  /** Produção registrada ou removida (a baixa no estoque muda). */
  baixaAlterada: boolean | null;
}

export const AvariaRequestSchema = z.object({ eventoId: z.number().int().positive(), insumoId: z.number().int().positive() });
export type AvariaRequestDTO = z.infer<typeof AvariaRequestSchema>;

/* ----------------------------- Ordens de produção ----------------------------- */

export interface SetorProducaoDTO {
  id: string;
  nome: string;
  ativo: boolean;
}

/** Ficha mestre da produção do evento (gerada e liberada pela gestão). */
export interface MapaProducaoDTO {
  gerado: string;
  /** "" = só a gestão vê (em teste). */
  liberado: string;
  responsavel: string;
  horarios: string;
}

export interface HistoricoOrdemDTO {
  em: string;
  quem: string;
  acao: string;
}

/** O que a equipe registrou na ordem. */
export interface ExecucaoOrdemDTO {
  iniciadoPor: string;
  iniciadoEm: string | null;
  concluidoPor: string;
  concluidoEm: string | null;
  qtd: number | null;
  perda: number;
  obs: string;
  conferidoPor: string;
  conferidoEm: string | null;
  lote: string;
  motivo: string;
  motivoTxt: string;
  hist: HistoricoOrdemDTO[];
}

/**
 * Ordem de produção gerada do cardápio confirmado e das fichas: principal (m_), sub-receita (s_),
 * finalização (f_) e expedição (x_exp). Prazo, setor e responsável podem ser ajustados no plano.
 */
export interface OrdemProducaoDTO {
  k: string;
  eventoId: number;
  codigo: string;
  eventoNome: string;
  eventoData: string;
  /** Item do cardápio (principal e finalização). */
  cid: number | null;
  prepId: number | null;
  nome: string;
  etapa: EtapaOrdem;
  setor: string;
  /** Quantidade necessária (0 na expedição). */
  q: number;
  u: string;
  rend: number;
  rendU: string;
  receitas: number;
  dn: number;
  /** Prazo é o padrão da ficha/praça (não ajustado no plano). */
  dnPadrao: boolean;
  /** Dia em que a ordem tem de estar pronta. */
  data: string | null;
  hora: string;
  resp: string;
  /** Pratos que usam esta ordem (sub-receitas). */
  origem: string[];
  tempoMin: number;
  equip: string;
  armaz: string;
  valDias: number;
  ins: { n: string; q: number; u: string; sub?: boolean }[];
  passos: string[];
  comp: { n: string; q: number; u: string }[];
  status: StatusOrdem;
  atrasada: boolean;
  exec: ExecucaoOrdemDTO;
  /** Etiqueta de identificação e rastreabilidade (ordem conferida). */
  etiqueta: string | null;
}

export interface LiberacaoExpedicaoDTO {
  eventoId: number;
  ck: Partial<Record<ItemLiberacao, boolean>>;
  horaSaida: string;
  autorizadoPor: string;
  autorizadoEm: string | null;
  comPendencias: boolean;
  pendencias: string;
  ordensProducao: number;
  ordensAbertas: number;
  checklistProntos: number;
  checklistTotal: number;
  /** Tudo pronto para liberar sem pendências. */
  ok: boolean;
  /** O que falta, em frases curtas. */
  faltam: string[];
  situacao: ["good" | "warn" | "accent" | "bad", string];
}

export interface OcorrenciaDTO {
  id: number;
  eventoId: number;
  tipo: TipoOcorrencia;
  quem: string;
  texto: string;
  acao: string;
  quando: string;
  resolvida: boolean;
}

export interface IndicadoresProducaoDTO {
  eventos: {
    eventoId: number;
    codigo: string;
    nome: string;
    ordens: number;
    concluidas: number;
    atrasadas: number;
    bloqueios: number;
    /** Realizado / previsto das ordens concluídas (null = nada concluído). */
    realizadoPrevisto: number | null;
    perdas: number;
    tempoReal: number;
    tempoEstimado: number;
    ocorrencias: number;
    /** Só na gestão. */
    custoPrevisto?: number;
    custoRealizado?: number;
  }[];
  setores: { setor: string; nome: string; ordens: number; concluidas: number; atrasadas: number; horas: number }[];
  /** Só na gestão: 15 maiores consumos de insumos pelas ordens conferidas. */
  consumo?: { nome: string; qtd: number; unidade: string }[];
}

const quem = () => texto(80).min(1, "Informe seu nome.");

/** Ações da equipe numa ordem. Cada ação grava no histórico com o nome de quem registrou. */
export const AcaoOrdemRequestSchema = z.discriminatedUnion("acao", [
  z.object({ acao: z.literal("iniciar"), eventoId: idPositivo(), k: z.string().min(1), quem: quem(), resp: texto(80).default("") }),
  z.object({
    acao: z.literal("concluir"),
    eventoId: idPositivo(),
    k: z.string().min(1),
    quem: quem(),
    qtd: z.number().positive("Informe a quantidade produzida.").nullable(),
    perda: z.number().min(0).default(0),
    obs: texto(2000).default(""),
  }),
  z.object({ acao: z.literal("conferir"), eventoId: idPositivo(), k: z.string().min(1), quem: quem() }),
  z.object({ acao: z.literal("bloquear"), eventoId: idPositivo(), k: z.string().min(1), quem: quem(), motivo: z.enum(chaves(MOTIVOS_BLOQUEIO)), texto: texto(500).min(1, "Descreva o impedimento.") }),
  z.object({ acao: z.literal("desbloquear"), eventoId: idPositivo(), k: z.string().min(1), quem: texto(80).default("") }),
  z.object({ acao: z.literal("reabrir"), eventoId: idPositivo(), k: z.string().min(1), quem: texto(80).default("") }),
  z.object({ acao: z.literal("responsavel"), eventoId: idPositivo(), k: z.string().min(1), quem: quem(), resp: texto(80).min(1, "Informe o responsável.") }),
]);
export type AcaoOrdemRequestDTO = z.infer<typeof AcaoOrdemRequestSchema>;
export type AcaoOrdem = AcaoOrdemRequestDTO["acao"];

/** Gestão: prazo, horário limite, setor e responsável de uma ordem. */
export const PlanoOrdemRequestSchema = z.object({
  eventoId: idPositivo(),
  k: z.string().min(1),
  dn: z.number().int().min(0).max(30),
  hora: z.string().regex(/^(\d{2}:\d{2})?$/, "Horário inválido.").default(""),
  setor: z.string().min(1, "Escolha o setor."),
  resp: texto(80).default(""),
});
export type PlanoOrdemRequestDTO = z.infer<typeof PlanoOrdemRequestSchema>;

/** Gestão: gerar o mapa, editar a ficha mestre e liberar/recolher para a equipe. */
export const GerarMapaRequestSchema = z.object({ eventoId: idPositivo() });
export type GerarMapaRequestDTO = z.infer<typeof GerarMapaRequestSchema>;
export const MestreMapaRequestSchema = z.object({ eventoId: idPositivo(), responsavel: texto(120).default(""), horarios: texto(300).default("") });
export type MestreMapaRequestDTO = z.infer<typeof MestreMapaRequestSchema>;
export const LiberarMapaRequestSchema = z.object({ eventoId: idPositivo(), liberar: z.boolean() });
export type LiberarMapaRequestDTO = z.infer<typeof LiberarMapaRequestSchema>;

export const SalvarSetoresRequestSchema = z.object({
  setores: z.array(z.object({ id: z.string().default(""), nome: texto(60).min(1, "Informe o nome do setor."), ativo: z.boolean().default(true) })).min(1),
});
export type SalvarSetoresRequestDTO = z.infer<typeof SalvarSetoresRequestSchema>;

/** Conferências e horário de saída da liberação (salvos campo a campo). */
export const SalvarLiberacaoRequestSchema = z.object({
  eventoId: idPositivo(),
  ck: z.record(z.enum(chaves(ITENS_LIBERACAO)), z.boolean()).optional(),
  horaSaida: z.string().regex(/^(\d{2}:\d{2})?$/, "Horário inválido.").optional(),
});
export type SalvarLiberacaoRequestDTO = z.infer<typeof SalvarLiberacaoRequestSchema>;
export const AutorizarLiberacaoRequestSchema = z.object({
  eventoId: idPositivo(),
  quem: quem(),
  /** Obrigatório quando a liberação não está ok. */
  pendencias: texto(2000).default(""),
});
export type AutorizarLiberacaoRequestDTO = z.infer<typeof AutorizarLiberacaoRequestSchema>;

export const CriarOcorrenciaRequestSchema = z.object({
  eventoId: idPositivo(),
  tipo: z.enum(TIPOS_OCORRENCIA),
  quem: texto(80).min(1, "Informe quem registra."),
  texto: texto(2000).min(1, "Descreva o que aconteceu."),
  acao: texto(2000).default(""),
});
export type CriarOcorrenciaRequestDTO = z.infer<typeof CriarOcorrenciaRequestSchema>;
export const ResolverOcorrenciaRequestSchema = z.object({ resolvida: z.boolean() });
export type ResolverOcorrenciaRequestDTO = z.infer<typeof ResolverOcorrenciaRequestSchema>;

