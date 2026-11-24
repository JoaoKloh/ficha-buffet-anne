package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ItemCardapioRequestDTO(
    @NotNull(message = "O evento do cardápio é obrigatório.")
    @Positive(message = "Evento inválido.")
    Long eventoId,
    @NotNull(message = "Escolha a preparação.")
    @Positive(message = "Escolha a preparação.")
    Long preparacaoId,
    @NotNull(message = "Informe a porção por convidado.")
    @Positive(message = "Informe a porção por convidado.")
    Double porcao,
    // Nula ou igual à da ficha: vale a praça da ficha.
    @Pattern(regexp = Dominio.RE_PRACA, message = "Praça inválida.")
    String praca,
    @Size(max = 100, message = "O responsável deve ter no máximo 100 caracteres.")
    String responsavel
) {}
