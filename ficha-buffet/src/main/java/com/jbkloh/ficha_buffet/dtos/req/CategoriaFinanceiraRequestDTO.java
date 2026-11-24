package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoriaFinanceiraRequestDTO(
    @NotBlank(message = "Informe o nome.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    String nome,
    @NotBlank(message = "Escolha o grupo da DRE.")
    @Pattern(regexp = Dominio.RE_GRUPO_DRE, message = "Grupo da DRE inválido.")
    String grupo,
    @PositiveOrZero(message = "A ordem não pode ser negativa.")
    Integer ordem
) {}
