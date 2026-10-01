package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateIngredienteRequestDto(

    @NotNull(message = "O id do ingrediente não pode ser nulo.") 
    Long id,
    
    @NotBlank(message = "O nome do ingrediente é obrigatório.")
    @Size(max = 255, message = "O nome do ingrediente deve ter no máximo 255 caracteres.")
    String nome,

    @NotBlank(message = "A unidade de medida do ingrediente é obrigatória.")
    @Size(max = 50, message = "A unidade de medida deve ter no máximo 50 caracteres.")
    String unidade,

    @NotBlank(message = "A categoria do ingrediente é obrigatória.")
    String categoria,

    @Size(max = 1024, message = "A unidade de medida deve ter no máximo 1024 caracteres.")
    String descricao,

    @Positive (message = "O custo não pode ser negativo.")
    Double custo,

    @Size(max = 50, message = "O fornecedo deve ter no máximo 50 caracteres.")
    String fornecedor
) {}