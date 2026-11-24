package com.jbkloh.ficha_buffet.dtos.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Linha da aba Itens: cadastro + em quantas fichas aparece. */
public record InsumoListaItemDTO(@JsonUnwrapped InsumoResponseDTO insumo, int emFichas) {}
