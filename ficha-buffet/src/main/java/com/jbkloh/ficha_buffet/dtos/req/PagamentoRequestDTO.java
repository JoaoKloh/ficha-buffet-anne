package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Pagamento ou recebimento de uma parte (ou do total) de um lançamento. */
public record PagamentoRequestDTO(
    @NotNull(message = "Informe o valor.")
    @Positive(message = "Informe o valor.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valor,
    @NotBlank(message = "Informe a data.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data.")
    String data,
    @Pattern(regexp = Dominio.RE_CONTA, message = "Conta inválida.")
    String conta,
    @Pattern(regexp = Dominio.RE_FORMA_PAGAMENTO, message = "Forma de pagamento inválida.")
    String forma
) {}
