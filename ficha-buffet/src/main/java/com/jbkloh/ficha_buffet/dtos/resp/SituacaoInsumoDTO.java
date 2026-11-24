package com.jbkloh.ficha_buffet.dtos.resp;

/** Diz se o item pode ser excluído: uso em fichas e movimentos de estoque. */
public record SituacaoInsumoDTO(int emFichas, boolean temMovimentos) {}
