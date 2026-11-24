package com.jbkloh.ficha_buffet.dtos.resp;

/** Perda de insumo, avaria de louça e utensílio ou descarte de pré-preparo, com valor. */
public record PerdaDTO(String data, String nome, double qtd, String unidade, double valor, String motivo, String grupo, String eventoNome, Long insumoId,
        Long loteId) {}
