package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CopiarCardapioRequestDTO(
    @NotNull(message = "Escolha o evento de origem.")
    @Positive(message = "Escolha o evento de origem.")
    Long origemEventoId
) {}
