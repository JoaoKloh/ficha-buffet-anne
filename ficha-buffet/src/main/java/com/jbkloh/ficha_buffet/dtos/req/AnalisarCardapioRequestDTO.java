package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalisarCardapioRequestDTO(
    @NotBlank(message = "Cole o cardápio primeiro.")
    @Size(max = 20000, message = "O texto do cardápio é grande demais.")
    String texto
) {}
