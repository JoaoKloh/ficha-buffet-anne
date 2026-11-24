package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Item de estoque no contrato do Anne Gestão. Campos opcionais nulos assumem o padrão do cadastro. */
public record InsumoRequestDTO(
    @NotBlank(message = "Informe o nome.")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres.")
    String nome,
    @Pattern(regexp = Dominio.RE_TIPO_ITEM, message = "Tipo de item inválido.")
    String tipo,
    @Pattern(regexp = Dominio.RE_CATEGORIA_INSUMO, message = "Categoria de item inválida.")
    String categoria,
    @Pattern(regexp = Dominio.RE_UNIDADE_INSUMO, message = "A unidade deve ser kg, l, un, dz, maço, garrafa ou botijão.")
    String unidade,
    @Pattern(regexp = Dominio.RE_LOCAL_ESTOQUE, message = "Local de estoque inválido.")
    String local,
    @Size(max = 60, message = "A unidade de compra deve ter no máximo 60 caracteres.")
    String unidadeCompra,
    @Positive(message = "O fator de compra deve ser maior que zero.")
    Double fatorCompra,
    @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
    Double estoqueMinimo,
    @PositiveOrZero(message = "O custo não pode ser negativo.")
    @Digits(integer = 11, fraction = 4, message = "O custo deve ter no máximo 4 casas decimais.")
    BigDecimal custo,
    @Size(max = 120, message = "O fornecedor deve ter no máximo 120 caracteres.")
    String fornecedor
) {}
