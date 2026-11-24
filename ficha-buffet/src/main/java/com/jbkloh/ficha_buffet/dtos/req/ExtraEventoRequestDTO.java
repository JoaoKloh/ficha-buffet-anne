package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExtraEventoRequestDTO(
    @NotNull(message = "O evento do extra é obrigatório.")
    @Positive(message = "Evento inválido.")
    Long eventoId,
    @NotBlank(message = "Descreva o extra.")
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
    String descricao,
    @NotNull(message = "Informe quantidade e valor.")
    @Positive(message = "Informe quantidade e valor.")
    Double quantidade,
    @NotNull(message = "Informe quantidade e valor.")
    @Positive(message = "Informe quantidade e valor.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valorUnitario
) {}
