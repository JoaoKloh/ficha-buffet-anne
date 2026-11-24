package com.jbkloh.ficha_buffet.dtos.resp;

/** Plano de logística do evento. Horários "HH:MM" ou "" (a definir); atualizadoEm nulo enquanto nunca foi salvo. */
public record LogisticaEventoDTO(String veiculos, String motorista, String viagens, String saida, String montagem, String retorno, String obs,
        String atualizadoEm) {

    public static final LogisticaEventoDTO VAZIA = new LogisticaEventoDTO("", "", "", "", "", "", "", null);
}
