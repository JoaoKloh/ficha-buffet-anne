package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Nova quantidade de um item ainda não concluído, na unidade de rendimento da ficha. */
public record AjustarItemPlanoRequestDTO(
    @NotNull(message = "Informe uma quantidade maior que zero.") @Positive(message = "Informe uma quantidade maior que zero.") Double qtd
) {}
