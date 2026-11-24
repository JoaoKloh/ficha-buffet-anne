package com.jbkloh.ficha_buffet.model.json;

/** Custos do evento por tipo (A&B, equipe, transporte, outros), previsto e realizado, guardados no retrato do fechamento. */
public record CustosPorTipo(Valores previsto, Valores realizado) {

    public record Valores(double ab, double equipe, double transporte, double outros) {}
}
