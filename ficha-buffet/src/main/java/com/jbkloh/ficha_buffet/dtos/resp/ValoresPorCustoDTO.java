package com.jbkloh.ficha_buffet.dtos.resp;

/** Valores por tipo de custo do evento (classificados pelo nome da categoria, como no protótipo). */
public record ValoresPorCustoDTO(double ab, double equipe, double transporte, double outros) {

    public double total() {
        return ab + equipe + transporte + outros;
    }

    public double de(String tipo) {
        return switch (tipo) {
            case "ab" -> ab;
            case "equipe" -> equipe;
            case "transporte" -> transporte;
            default -> outros;
        };
    }
}
