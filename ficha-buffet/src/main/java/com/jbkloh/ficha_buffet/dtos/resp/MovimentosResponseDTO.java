package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Movimentos filtrados (até 300) e o total do filtro. */
public record MovimentosResponseDTO(List<MovimentoListaItemDTO> movimentos, int total) {}
