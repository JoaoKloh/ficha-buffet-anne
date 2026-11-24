package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Carta do restaurante: KPIs de CMV e pratos com custo calculado. */
public record PratosVendaResponseDTO(Kpis kpis, List<PratoVendaListaItemDTO> pratos) {

    public record Kpis(int ativos, int foraDaCarta, Double cmvMedio, int acimaDaMeta, int incompletos) {}
}
