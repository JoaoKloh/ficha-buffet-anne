package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Rascunho da ficha, para o custo ao vivo do formulário. */
public record CalcularCustoRequestDTO(
    List<@Valid @NotNull Item> itens,
    @Positive(message = "O rendimento deve ser maior que zero.") Double rendimento
) {
    public record Item(
        @Positive(message = "Item da receita inválido.") Long insumoId,
        @Positive(message = "Sub-receita inválida.") Long subId,
        @PositiveOrZero(message = "A quantidade não pode ser negativa.") Double qtd,
        @Positive(message = "O fator de correção deve ser maior que zero.") Double fc
    ) {}
}
