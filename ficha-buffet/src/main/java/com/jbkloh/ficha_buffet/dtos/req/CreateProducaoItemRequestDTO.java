package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;

public record CreateProducaoItemRequestDTO(

    @NotNull(message = "O ID do prato é obrigatório.")
    Long pratoId
) {}