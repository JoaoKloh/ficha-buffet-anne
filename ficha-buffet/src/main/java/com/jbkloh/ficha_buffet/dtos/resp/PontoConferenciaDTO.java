package com.jbkloh.ficha_buffet.dtos.resp;

/** Ponto para conferir no estoque. tom: warn ou bad; o id preenchido diz o que abrir (item, lote ou nota). */
public record PontoConferenciaDTO(String tom, String texto, Long insumoId, Long loteId, Long nfId) {}
