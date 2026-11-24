package com.jbkloh.ficha_buffet.dtos.resp;

/**
 * Indicadores do evento (evProjeto do protótipo). contratado: receitas do evento (parcelas e extras,
 * canceladas fora); receitaPrevista: contratado ou, sem receita lançada, o valor do contrato;
 * comExtras: contratado passou do valor do contrato. previsto: A&B pelas fichas, equipe pela
 * escala (sem escala, pelo orçamento do evento), transporte e outros pelo orçamento. realizado: custos lançados
 * no evento por tipo, mais a saída líquida do estoque geral (estoqueGeral, em A&B) e as avarias de
 * louça (em outros). convidados: contratados, ou previstos sem contrato. cmv*: null sem receita.
 */
public record IndicadoresEventoDTO(double contratado, double receitaPrevista, boolean comExtras, double recebido, double aReceber,
        ValoresPorCustoDTO previsto, ValoresPorCustoDTO realizado, double totalPrevisto, double totalRealizado, int convidados, double estoqueGeral,
        double avarias, double aPagar, int contasAPagar, double resultadoPrevisto, double resultadoRealizado, Double cmvPrevisto, Double cmvRealizado) {}
