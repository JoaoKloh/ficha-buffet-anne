package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssociationPratoProducaoRequestDTO(
    
    @NotNull(message = "O ID do prato é obrigatório.")
    Long pratoId,

    @NotNull(message = "É necessário vincular um ou mais eventos.")
    List<Long> eventosId 
) {
    
}
