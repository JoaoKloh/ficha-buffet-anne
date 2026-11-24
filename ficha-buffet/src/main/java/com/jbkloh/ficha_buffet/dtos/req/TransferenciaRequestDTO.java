package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Transferência entre unidades: uma unidade usou produto ou insumo comprado por outra. */
public record TransferenciaRequestDTO(
    @NotBlank(message = "Informe a data.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data.")
    String data,
    @Size(max = 100, message = "O responsável deve ter no máximo 100 caracteres.")
    String responsavel,
    @NotBlank(message = "Escolha de qual unidade sai.")
    @Pattern(regexp = Dominio.RE_UNIDADE, message = "Unidade de origem inválida.")
    String unidadeOrigem,
    @NotBlank(message = "Escolha para qual unidade vai.")
    @Pattern(regexp = Dominio.RE_UNIDADE, message = "Unidade de destino inválida.")
    String unidadeDestino,
    @Positive(message = "Item do cadastro inválido.")
    Long insumoId,
    @Size(max = 200, message = "O produto deve ter no máximo 200 caracteres.")
    String item,
    @PositiveOrZero(message = "A quantidade não pode ser negativa.")
    Double qtd,
    @Size(max = 20, message = "A unidade de medida deve ter no máximo 20 caracteres.")
    String unid,
    @NotNull(message = "Informe o custo da transferência.")
    @Positive(message = "Informe o custo da transferência.")
    @Digits(integer = 13, fraction = 2, message = "O custo deve ter no máximo 2 casas decimais.")
    BigDecimal valor,
    @NotNull(message = "Escolha a linha de custo.")
    @Positive(message = "Escolha a linha de custo.")
    Long categoriaId,
    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
    String obs
) {}
