package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssociationIngredientePratoRequestDTO(
    
    @NotNull(message = "O ID do ingrediente é obrigatório.")
    Long ingredienteId,

    @NotNull(message = "É necessário vincular um ou mais pratos.")
    List<Long> pratosId 
) {
    
}
