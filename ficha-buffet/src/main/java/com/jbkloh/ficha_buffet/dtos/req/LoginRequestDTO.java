package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(

    @NotNull(message = "É necessário inserir o usuário.")
    String username,

    @NotNull (message = "É necessário inserir a senha.")
    String password
) {
    
}
