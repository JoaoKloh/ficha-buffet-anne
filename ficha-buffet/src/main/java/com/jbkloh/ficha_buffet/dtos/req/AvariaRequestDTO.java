package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AvariaRequestDTO(
    @NotNull(message = "O evento é obrigatório.") @Positive(message = "Evento inválido.") Long eventoId,
    @NotNull(message = "O item é obrigatório.") @Positive(message = "Item inválido.") Long insumoId
) {}
