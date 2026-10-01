package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarLancamentoDTO(

    @NotNull(message = "O id do lançamento não pode ser nulo.")
    Long id,

    @NotBlank(message = "O tipo do lançamento é obrigatório.")
    @Pattern(regexp = "receita|despesa", message = "O tipo do lançamento deve ser 'receita' ou 'despesa'.")
    String tipo,

    @NotBlank(message = "A categoria do lançamento é obrigatória.")
    @Size(max = 100, message = "A categoria deve ter no máximo 100 caracteres.")
    String categoria,

    @NotBlank(message = "A descrição do lançamento é obrigatória.")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
    String descricao,

    @NotNull(message = "O valor do lançamento é obrigatório.")
    @Positive(message = "O valor do lançamento deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valor,

    @NotNull(message = "A data do lançamento é obrigatória.")
    LocalDate data
) {}
