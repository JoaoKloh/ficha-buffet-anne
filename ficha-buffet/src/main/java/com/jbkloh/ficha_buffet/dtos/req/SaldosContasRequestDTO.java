package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;
import java.util.Map;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Saldo de cada conta no fim do dia `data` (do extrato). Conta sem valor fica sem saldo informado. */
public record SaldosContasRequestDTO(
    @NotBlank(message = "Informe a data.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data.")
    String data,
    @NotNull(message = "Informe os saldos.")
    Map<@Pattern(regexp = Dominio.RE_CONTA, message = "Conta inválida.") String, BigDecimal> contas
) {}
