package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Percentual de cada unidade operacional no rateio da estrutura compartilhada. Somam 100. */
public record RateioRequestDTO(
    @NotNull(message = "Informe o percentual do Buffet.") @PositiveOrZero(message = "Percentual inválido.") Double buffet,
    @NotNull(message = "Informe o percentual do Restaurante.") @PositiveOrZero(message = "Percentual inválido.") Double restaurante,
    @NotNull(message = "Informe o percentual da Chocolates.") @PositiveOrZero(message = "Percentual inválido.") Double chocolates
) {}
