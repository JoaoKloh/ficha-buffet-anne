package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportarXmlRequestDTO(
    @NotBlank(message = "Arquivo vazio.")
    @Size(max = 5 * 1024 * 1024, message = "Arquivo grande demais para um XML de NF-e.")
    String xml
) {}
