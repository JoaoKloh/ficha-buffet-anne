import type { StatusEvento } from "./dominio";
import type { EventoListaItemDTO } from "./evento";

/* ------------------------------- Tela do Painel ------------------------------- */

export type AlertaTom = "bad" | "warn" | "info";
/** Trecho do texto do alerta; `{ b }` sai em negrito. */
export type AlertaParte = string | { b: string };
/** Para onde o alerta leva: ficha do evento, outra tela ou a folha de backup. */
export type AlertaDestino = { tipo: "evento"; id: number } | { tipo: "href"; href: string } | { tipo: "acao"; acao: "backup" };

export interface AlertaDTO {
  tom: AlertaTom;
  ord: number;
  partes: AlertaParte[];
  destino: AlertaDestino;
}

/**
 * faturamento: competência do mês com previstos; recebido: regime de caixa.
 * aReceber: saldo das receitas em aberto vencendo em até 30 dias; aPagar: das despesas em até 7 dias.
 */
export interface PainelKpisDTO {
  faturamento: number;
  recebido: number;
  aReceber: number;
  contasReceber: number;
  receberVencidas: number;
  receberVencido: number;
  aPagar: number;
  contasPagar: number;
  pagarVencidas: number;
  pagarVencido: number;
  resultado: number;
  margem: number | null;
}

/** Resultado do mês de cada unidade operacional (competência, com previstos e estrutura rateada). */
export interface PainelUnidadesDTO {
  linhas: {
    unidade: "buffet" | "restaurante" | "chocolates";
    rotulo: string;
    faturamento: number;
    cmv: number;
    pctCmv: number | null;
    margemContribuicao: number;
    resultado: number;
    margem: number | null;
  }[];
  semUnidadeMes: number;
  rateio: Record<"buffet" | "restaurante" | "chocolates", number>;
}

export interface PainelGraficosDTO {
  /** 12 meses "AAAA-MM": 5 para trás, o atual e 6 à frente. */
  meses: string[];
  /** Posição do mês atual em `meses`; depois dela é futuro. */
  mesAtual: number;
  receitas: number[];
  despesas: number[];
  resultado: number[];
  /** 12 meses a partir do atual. */
  mesesEventos: string[];
  eventos: number[];
  convidados: number[];
  funil: { status: StatusEvento; rotulo: string; n: number }[];
  totalEventos: number;
  contratadosOuAdiante: number;
  /** Últimos 4 meses, maiores primeiro; além de 6 categorias, as menores viram "Outras". */
  despesasCategoria: { nome: string; valor: number }[];
  despesasDe: string;
}

/** KPIs de operação do Painel (protótipo: painelOperacao). Cada um leva à tela que resolve. */
export interface PainelOperacaoDTO {
  /** Consumíveis com saldo negativo, disponível negativo ou abaixo do mínimo. */
  estoqueCritico: { n: number; nomes: string[] };
  /** Insumos e pré-preparos vencendo em 7 dias (já vencidos inclusos). */
  vencendo: { n: number; vencidos: number };
  /** Pratos e tarefas da cozinha com prazo vencido. */
  producoesAtrasadas: { n: number; eventos: number };
  /** Sugestão de compras para 7 dias ainda sem pedido. */
  comprasPendentes: { n: number; valor: number };
  /** Eventos nos próximos 30 dias com etapa crítica pendente ou com desvio de custo. */
  eventosComPendencias: number;
  /** A&B do mês ÷ receita do mês (competência, com previstos). */
  cmv: number | null;
  metaCmv: number;
  margem: number | null;
}

export interface PainelResumoResponseDTO {
  hoje: string;
  mes: string;
  kpis: PainelKpisDTO;
  alertas: AlertaDTO[];
  proximos: EventoListaItemDTO[];
  graficos: PainelGraficosDTO;
  unidades: PainelUnidadesDTO;
  /** Opcional até o backend devolver. */
  operacao?: PainelOperacaoDTO | null;
}
