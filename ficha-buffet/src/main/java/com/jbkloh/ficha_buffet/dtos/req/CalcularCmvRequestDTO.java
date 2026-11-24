package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Rascunho do prato de venda, para o custo e o CMV ao vivo do formulário. */
public record CalcularCmvRequestDTO(
    List<@Valid @NotNull Componente> componentes,
    List<@Valid @NotNull Extra> extras,
    @PositiveOrZero(message = "O preço não pode ser negativo.") Double preco
) {
    public record Componente(
        @NotNull(message = "Ficha técnica inválida.") @Positive(message = "Ficha técnica inválida.") Long preparacaoId,
        @NotNull(message = "Informe a quantidade.") @PositiveOrZero(message = "A quantidade não pode ser negativa.") Double qtd
    ) {}

    public record Extra(
        @NotNull(message = "Insumo inválido.") @Positive(message = "Insumo inválido.") Long insumoId,
        @NotNull(message = "Informe a quantidade.") @PositiveOrZero(message = "A quantidade não pode ser negativa.") Double qtd
    ) {}
}
