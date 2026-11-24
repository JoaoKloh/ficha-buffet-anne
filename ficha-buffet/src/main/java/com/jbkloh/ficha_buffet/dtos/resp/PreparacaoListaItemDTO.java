package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Linha da aba Preparações: ficha + custo calculado + situação. */
public record PreparacaoListaItemDTO(@JsonUnwrapped PreparacaoResponseDTO preparacao, CustoPreparacaoDTO custo, String fichaStatus,
        boolean usadaEmCardapio, List<String> usadaComoSub) {}
