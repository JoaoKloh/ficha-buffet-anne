package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Fichas marcadas na seleção múltipla do cardápio. */
public record AdicionarPratosCardapioRequestDTO(
    @NotEmpty(message = "Marque ao menos um prato.")
    List<@NotNull(message = "Ficha técnica inválida.") @Positive(message = "Ficha técnica inválida.") Long> preparacaoIds
) {}
