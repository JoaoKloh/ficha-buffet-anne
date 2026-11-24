package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record InventarioRequestDTO(
    @NotEmpty(message = "Nenhum item com diferença.")
    List<@Valid @NotNull Contagem> contagens
) {
    public record Contagem(
        @NotNull(message = "Item da contagem inválido.") @Positive(message = "Item da contagem inválido.") Long insumoId,
        @NotNull(message = "Informe a quantidade contada.") @PositiveOrZero(message = "A contagem não pode ser negativa.") Double contado
    ) {}
}
