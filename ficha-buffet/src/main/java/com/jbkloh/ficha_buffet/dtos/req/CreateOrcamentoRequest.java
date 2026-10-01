package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateOrcamentoRequest(

    @NotBlank(message = "O nome do cliente é obrigatório.")
    @Size(max = 255, message = "O nome do cliente deve ter no máximo 255 caracteres.")
    String nomeCliente,

    // Opcional na aba Evento do frontend.
    @Size(max = 255, message = "O contato do cliente deve ter no máximo 255 caracteres.")
    String contatoCliente,

    // Opcional na aba Evento do frontend.
    @Size(max = 255, message = "O tipo de serviço deve ter no máximo 255 caracteres.")
    String tipoServico,

    @NotNull(message = "A duração do evento é obrigatória.")
    @PositiveOrZero(message = "A duração do evento não pode ser negativa.")
    Double duracaoEvento,

    @NotNull(message = "O número de convidados é obrigatório.")
    @Positive(message = "O número de convidados deve ser maior que zero.")
    Integer numeroConvidados,

    @NotNull(message = "O valor de alimentos é obrigatório.")
    @PositiveOrZero(message = "O valor de alimentos não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor de alimentos deve ter no máximo 2 casas decimais.")
    BigDecimal valorAlimentos,

    @NotNull(message = "O valor da equipe é obrigatório.")
    @PositiveOrZero(message = "O valor da equipe não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor da equipe deve ter no máximo 2 casas decimais.")
    BigDecimal valorEquipe,

    @NotNull(message = "O valor da degustação é obrigatório.")
    @PositiveOrZero(message = "O valor da degustação não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor da degustação deve ter no máximo 2 casas decimais.")
    BigDecimal valorDegustacao,

    @NotNull(message = "O valor de outros custos é obrigatório.")
    @PositiveOrZero(message = "O valor de outros custos não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor de outros custos deve ter no máximo 2 casas decimais.")
    BigDecimal valorOutros,

    @NotNull(message = "O valor total é obrigatório.")
    @PositiveOrZero(message = "O valor total não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor total deve ter no máximo 2 casas decimais.")
    BigDecimal valorTotal,

    @NotNull(message = "O valor por pessoa é obrigatório.")
    @PositiveOrZero(message = "O valor por pessoa não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor por pessoa deve ter no máximo 2 casas decimais.")
    BigDecimal valorPorPessoa
) {}
