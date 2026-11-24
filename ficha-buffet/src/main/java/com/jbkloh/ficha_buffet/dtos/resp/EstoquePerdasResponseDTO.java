package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Perdas, avarias e descartes do mês (AAAA-MM), mais recentes primeiro. */
public record EstoquePerdasResponseDTO(String mes, Kpis kpis, List<PorGrupo> porGrupo, List<PerdaDTO> perdas) {

    /** pctCompras: perdas ÷ entradas do mês (nulo sem entradas); maiorCausa: motivo de maior valor (nulo sem perdas). */
    public record Kpis(double total, int registros, double comprasMes, Double pctCompras, double sobras, int sobrasRegistros, String maiorCausa) {}

    public record PorGrupo(String grupo, double valor) {}
}
