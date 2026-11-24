package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Nota fiscal de compra como vem do formulário (rascunho ou entrada). */
public record NotaFiscalRequestDTO(
    @NotBlank(message = "Informe o fornecedor.")
    @Size(max = 120, message = "O fornecedor deve ter no máximo 120 caracteres.")
    String fornecedor,
    @Size(max = 20, message = "O número da nota deve ter no máximo 20 caracteres.")
    String numero,
    @NotBlank(message = "Informe a data de emissão.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data de emissão.")
    String emissao,
    @Positive(message = "Evento inválido.")
    Long eventoId,
    Boolean lancarCaixa,
    @Positive(message = "Categoria inválida.")
    Long categoriaId,
    @Pattern(regexp = Dominio.RE_FORMA_PAGAMENTO_NF, message = "Forma de pagamento inválida.")
    String formaPag,
    List<@Valid @NotNull Parcela> parcelas,
    List<@Valid @NotNull Item> itens,
    @Digits(integer = 13, fraction = 2, message = "O acréscimo deve ter no máximo 2 casas decimais.")
    BigDecimal acrescimo,
    @Pattern(regexp = "^$|\\d{44}", message = "A chave de acesso deve ter 44 dígitos.")
    String chave,
    @Size(max = 10, message = "A série deve ter no máximo 10 caracteres.")
    String serie,
    @Size(max = 200, message = "A razão social deve ter no máximo 200 caracteres.")
    String razaoSocial,
    @Size(max = 18, message = "O CNPJ deve ter no máximo 18 caracteres.")
    String cnpjFornecedor,
    @JsonProperty("importadaXML") Boolean importadaXML,
    Boolean importadaFoto,
    @Pattern(regexp = "^$|" + Dominio.RE_UNIDADE, message = "Unidade de negócio inválida.")
    String unidade,
    // Dar entrada mesmo com a soma das parcelas diferente do total dos produtos: a diferença vira acréscimo ou desconto.
    Boolean aceitarDiferenca
) {
    public record Parcela(
        @Pattern(regexp = Dominio.RE_DATA_OU_VAZIA, message = "Vencimento de parcela inválido.") String venc,
        @PositiveOrZero(message = "O valor da parcela não pode ser negativo.") Double valor,
        Boolean pago
    ) {}

    public record Item(
        @Size(max = 200, message = "A descrição da linha deve ter no máximo 200 caracteres.") String desc,
        @Size(max = 60, message = "O código da linha deve ter no máximo 60 caracteres.") String cod,
        @JsonProperty("uCom") @Size(max = 10, message = "A unidade da linha deve ter no máximo 10 caracteres.") String uCom,
        @Positive(message = "Item do cadastro inválido.") Long insumoId,
        @PositiveOrZero(message = "A quantidade não pode ser negativa.") Double qtdNota,
        @Pattern(regexp = "estoque|compra", message = "A unidade da linha deve ser estoque ou compra.") String unid,
        @PositiveOrZero(message = "O valor unitário não pode ser negativo.") Double valorUnit,
        @PositiveOrZero(message = "O valor da linha não pode ser negativo.") Double valor,
        Boolean conferido
    ) {}
}
