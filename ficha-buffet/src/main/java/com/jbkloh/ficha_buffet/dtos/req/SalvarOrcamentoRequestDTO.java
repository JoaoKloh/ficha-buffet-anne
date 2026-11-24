package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Orçamento do evento (custos previstos fora das fichas técnicas). equipe nula quando a equipe vem
 * da escala: o valor já gravado é mantido, para valer de novo se a escala for desfeita.
 */
public record SalvarOrcamentoRequestDTO(
    @PositiveOrZero(message = "O valor não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal equipe,
    @NotNull(message = "Informe o transporte.")
    @PositiveOrZero(message = "O valor não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal transporte,
    @NotNull(message = "Informe locação e outros.")
    @PositiveOrZero(message = "O valor não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal outros
) {}
