package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Põe os lançamentos indicados numa unidade de negócio. */
public record ClassificarUnidadeRequestDTO(
    @NotBlank(message = "Escolha a unidade.")
    @Pattern(regexp = Dominio.RE_UNIDADE, message = "Unidade de negócio inválida.")
    String unidade,
    @NotEmpty(message = "Nenhum lançamento para classificar.")
    List<@NotNull @Positive Long> ids
) {}
