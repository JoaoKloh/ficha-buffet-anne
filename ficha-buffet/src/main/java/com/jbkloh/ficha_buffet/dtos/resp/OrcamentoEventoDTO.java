package com.jbkloh.ficha_buffet.dtos.resp;

/** Orçamento do evento. equipe só vale enquanto não há escala (nula se nunca informada); atualizadoEm nulo enquanto nunca foi salvo. */
public record OrcamentoEventoDTO(Double equipe, double transporte, double outros, String atualizadoEm) {

    public static final OrcamentoEventoDTO VAZIO = new OrcamentoEventoDTO(null, 0, 0, null);
}
