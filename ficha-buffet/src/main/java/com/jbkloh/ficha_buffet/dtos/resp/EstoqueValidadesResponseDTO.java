package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Insumos e pré-preparos vencidos ou vencendo em 7 dias, do que vence antes para o que vence depois. */
public record EstoqueValidadesResponseDTO(Kpis kpis, List<ValidadeItemDTO> itens) {

    /** semValidade: insumos consumíveis com saldo e nenhuma entrada com validade. */
    public record Kpis(int vencidos, int vencem, double valorRisco, int semValidade) {}
}
