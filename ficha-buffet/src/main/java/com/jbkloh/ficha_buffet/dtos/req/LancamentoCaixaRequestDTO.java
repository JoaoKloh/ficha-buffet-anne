package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Lançamento manual do caixa: categoria pelo id, unidade de negócio e situação em aberto (previsto) ou paga (realizado). */
public record LancamentoCaixaRequestDTO(
    @NotBlank(message = "Descreva o lançamento.")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
    String descricao,
    @NotNull(message = "Informe o valor.")
    @Positive(message = "O valor deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valor,
    @NotNull(message = "Escolha a categoria.")
    @Positive(message = "Escolha a categoria.")
    Long categoriaId,
    @Positive(message = "Evento inválido.")
    Long eventoId,
    @NotBlank(message = "Informe a competência.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a competência.")
    String dataCompetencia,
    @NotBlank(message = "Informe se o lançamento é previsto ou realizado.")
    @Pattern(regexp = "previsto|realizado", message = "A situação deve ser previsto ou realizado.")
    String status,
    @Pattern(regexp = Dominio.RE_DATA, message = "Vencimento inválido.")
    String vencimento,
    @Pattern(regexp = Dominio.RE_DATA, message = "Data de pagamento inválida.")
    String dataPagamento,
    @Pattern(regexp = Dominio.RE_CONTA, message = "Conta inválida.")
    String conta,
    @NotBlank(message = "Escolha a unidade de negócio.")
    @Pattern(regexp = Dominio.RE_UNIDADE, message = "Unidade de negócio inválida.")
    String unidade,
    @Size(max = 120, message = "O fornecedor deve ter no máximo 120 caracteres.")
    String fornecedor
) {}
