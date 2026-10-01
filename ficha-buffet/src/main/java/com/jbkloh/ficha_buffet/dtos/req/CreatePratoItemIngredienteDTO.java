package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePratoItemIngredienteDTO(

    @NotNull(message = "O ID do ingrediente é obrigatório.")
    Long ingredienteId,

    @NotNull(message = "A quantidade do ingrediente é obrigatória.")
    @Positive(message = "A quantidade do ingrediente deve ser maior que zero.")
    Double qtd,

    @Size(max = 50, message = "A unidade do ingrediente deve ter no máximo 50 caracteres.")
    String unidade

) {}