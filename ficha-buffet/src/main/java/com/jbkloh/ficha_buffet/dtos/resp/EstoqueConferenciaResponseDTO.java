package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Integridade do estoque: movimentos (gravados e baixas calculadas) por origem e pontos para conferir. */
public record EstoqueConferenciaResponseDTO(List<PorOrigem> porOrigem, List<PontoConferenciaDTO> pontos) {

    public record PorOrigem(String origem, int n) {}
}
