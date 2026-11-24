package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Fichas marcadas para o plano, com a quantidade na unidade de rendimento de cada ficha. */
public record AdicionarItensPlanoRequestDTO(
    @NotEmpty(message = "Marque ao menos um item.") @Size(max = 500, message = "O plano aceita até 500 itens por vez.")
    List<@Valid @NotNull Item> itens
) {
    public record Item(
        @NotNull(message = "Escolha a ficha.") @Positive(message = "Escolha a ficha.") Long preparacaoId,
        @NotNull(message = "Informe a quantidade de todos os itens marcados.") @Positive(message = "Informe a quantidade de todos os itens marcados.") Double qtd
    ) {}
}
