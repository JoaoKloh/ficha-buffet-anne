package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;
import java.util.List;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PratoVendaRequestDTO(
    @NotBlank(message = "Informe o nome do prato.")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres.")
    String nome,
    @Pattern(regexp = Dominio.RE_SECAO_CARTA, message = "Seção da carta inválida.")
    String secao,
    @PositiveOrZero(message = "O preço não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais.")
    BigDecimal preco,
    @Size(max = 40, message = "O código do PDV deve ter no máximo 40 caracteres.")
    String codigoPdv,
    Boolean ativo,
    @Size(max = 2000, message = "A observação deve ter no máximo 2000 caracteres.")
    String obs,
    List<@Valid @NotNull Componente> componentes,
    List<@Valid @NotNull Extra> extras
) {
    @AssertTrue(message = "Ligue pelo menos uma ficha técnica ou insumo ao prato.")
    public boolean isComposto() {
        return (componentes != null && !componentes.isEmpty()) || (extras != null && !extras.isEmpty());
    }

    public record Componente(
        @NotNull(message = "Escolha a ficha técnica.") @Positive(message = "Ficha técnica inválida.") Long preparacaoId,
        @NotNull(message = "Informe a quantidade por prato de cada item.")
        @Positive(message = "Informe a quantidade por prato de cada item.")
        Double qtd
    ) {}

    public record Extra(
        @NotNull(message = "Escolha o insumo.") @Positive(message = "Insumo inválido.") Long insumoId,
        @NotNull(message = "Informe a quantidade por prato de cada item.")
        @Positive(message = "Informe a quantidade por prato de cada item.")
        Double qtd
    ) {}
}
