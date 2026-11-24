package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Contrato do evento. Os campos do grupo AoRegistrar geram as parcelas e só valem no registro. */
public record ContratoRequestDTO(
    @Size(max = 40, message = "O número do contrato deve ter no máximo 40 caracteres.")
    String numero,
    @NotBlank(message = "Informe a data de assinatura.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data de assinatura.")
    String dataAssinatura,
    @NotNull(message = "Informe os convidados contratados.")
    @Positive(message = "Informe os convidados contratados.")
    Integer convidadosContratados,
    @NotNull(message = "Informe o valor por convidado.")
    @PositiveOrZero(message = "O valor não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valorPorConvidado,
    @NotNull(message = "Informe o valor total.")
    @Positive(message = "Informe o valor total.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valorTotal,
    @NotNull(message = "Informe o valor por convidado extra.")
    @PositiveOrZero(message = "O valor não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valorConvidadoExtra,
    @NotNull(message = "Crie uma categoria de receita em Caixa › Categorias.")
    @Positive(message = "Crie uma categoria de receita em Caixa › Categorias.")
    Long categoriaId,
    @NotNull(groups = AoRegistrar.class, message = "Informe ao menos uma parcela.")
    @Min(value = 1, message = "Informe ao menos uma parcela.")
    @Max(value = 48, message = "O contrato aceita até 48 parcelas.")
    Integer nParcelas,
    @NotBlank(groups = AoRegistrar.class, message = "Informe o primeiro vencimento.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe o primeiro vencimento.")
    String primeiroVencimento,
    @NotNull(groups = AoRegistrar.class, message = "Informe o intervalo entre as parcelas.")
    @Min(value = 0, message = "O intervalo entre parcelas deve ser de 0, 1 ou 2 meses.")
    @Max(value = 2, message = "O intervalo entre parcelas deve ser de 0, 1 ou 2 meses.")
    Integer intervaloMeses
) {
    /** Grupo de validação do registro do contrato (criação das parcelas). */
    public interface AoRegistrar {}
}
