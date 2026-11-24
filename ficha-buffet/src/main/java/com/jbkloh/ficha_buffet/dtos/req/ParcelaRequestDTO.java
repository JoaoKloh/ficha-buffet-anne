package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ParcelaRequestDTO(
    @NotBlank(message = "Informe o vencimento.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe o vencimento.")
    String vencimento,
    @NotNull(message = "Informe o valor.")
    @Positive(message = "O valor deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valor,
    // Nulo: parcela em aberto.
    @Pattern(regexp = Dominio.RE_DATA, message = "Data de pagamento inválida.")
    String pagoEm,
    @Pattern(regexp = Dominio.RE_FORMA_RECEBIMENTO, message = "Forma de recebimento inválida.")
    String forma,
    @Pattern(regexp = Dominio.RE_CONTA, message = "A conta deve ser banco ou caixa.")
    String conta
) {}
