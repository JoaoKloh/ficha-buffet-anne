package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Situação (sim, definir, nao) e fator por convidado, alterados direto na lista. */
public record PatchItemCardapioRequestDTO(
    @Pattern(regexp = Dominio.RE_STATUS_ITEM_CARDAPIO, message = "Situação do prato inválida.")
    String status,
    @Positive(message = "O fator por convidado precisa ser maior que zero.")
    Double porcao
) {}
