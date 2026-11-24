package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Movimento manual de estoque. Em ajuste, qtd é a contagem real do item.
 * validade vale só para entrada; unidadeUso e compradoPor, só para consumo
 * (se outra unidade pagou a compra, o custo vira transferência para quem usou).
 */
public record MovimentoEstoqueRequestDTO(
    @NotBlank(message = "Escolha o tipo do movimento.")
    @Pattern(regexp = Dominio.RE_MOVIMENTO_MANUAL, message = "Tipo de movimento inválido.")
    String tipo,
    @NotNull(message = "Escolha o item.")
    @Positive(message = "Escolha o item.")
    Long insumoId,
    @NotBlank(message = "Informe a data.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data.")
    String data,
    @NotNull(message = "Informe a quantidade.")
    @PositiveOrZero(message = "A quantidade não pode ser negativa.")
    Double qtd,
    @PositiveOrZero(message = "O custo não pode ser negativo.")
    @Digits(integer = 11, fraction = 4, message = "O custo deve ter no máximo 4 casas decimais.")
    BigDecimal custoUnit,
    @Positive(message = "Evento inválido.")
    Long eventoId,
    @Size(max = 300, message = "O motivo deve ter no máximo 300 caracteres.")
    String motivo,
    @Pattern(regexp = Dominio.RE_DATA, message = "Validade inválida.")
    String validade,
    @Pattern(regexp = Dominio.RE_UNIDADE_OPERACIONAL, message = "Unidade que usou inválida.")
    String unidadeUso,
    @Pattern(regexp = Dominio.RE_UNIDADE, message = "Unidade que comprou inválida.")
    String compradoPor
) {
    @AssertTrue(message = "Escolha a unidade que usou.")
    public boolean isUnidadeDoConsumoInformada() {
        return !"consumo".equals(tipo) || unidadeUso != null;
    }

    @AssertTrue(message = "Quem comprou e quem usou são a mesma unidade: deixe \"A mesma unidade\".")
    public boolean isCompraDeOutraUnidade() {
        return compradoPor == null || !compradoPor.equals(unidadeUso);
    }
}
