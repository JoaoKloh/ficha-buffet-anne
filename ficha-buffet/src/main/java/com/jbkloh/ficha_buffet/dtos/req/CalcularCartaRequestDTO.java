package com.jbkloh.ficha_buffet.dtos.req;

import java.util.Map;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Pratos da carta previstos no período: pratoVendaId -> quantidade de pratos. */
public record CalcularCartaRequestDTO(
    @NotNull(message = "Informe ao menos um prato previsto.")
    Map<String, @NotNull(message = "Quantidade de pratos inválida.") @PositiveOrZero(message = "Quantidade de pratos inválida.") Integer> previsao
) {
    @AssertTrue(message = "Prato da carta inválido.")
    public boolean isChavesValidas() {
        return previsao == null || previsao.keySet().stream().allMatch(k -> k != null && k.matches("\\d+"));
    }

    @AssertTrue(message = "Informe ao menos um prato previsto.")
    public boolean isAlgumPrevisto() {
        return previsao == null || previsao.values().stream().anyMatch(n -> n != null && n > 0);
    }
}
