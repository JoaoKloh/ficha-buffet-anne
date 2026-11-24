package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/**
 * Retrato guardado ao fechar o evento: em (ISO) e por quem, receita e recebido, custos previsto e
 * realizado, resultado realizado, CMV realizado (3 casas, nulo sem receita), custos por tipo e as
 * etapas que estavam sem marcar.
 */
public record FechamentoEventoDTO(String em, String por, double receita, double recebido, double custoPrevisto, double custoRealizado, double resultado,
        Double cmv, PorTipo porTipo, List<String> pendencias) {

    public record PorTipo(ValoresPorCustoDTO previsto, ValoresPorCustoDTO realizado) {}
}
