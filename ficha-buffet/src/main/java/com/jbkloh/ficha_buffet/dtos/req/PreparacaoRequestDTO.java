package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Ficha técnica completa: receita, louças, etapas de produção e componentes de envio. */
public record PreparacaoRequestDTO(
    @NotBlank(message = "Informe o nome da preparação.")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres.")
    String nome,
    @NotBlank(message = "Escolha a categoria.")
    @Pattern(regexp = Dominio.RE_CATEGORIA_PREPARACAO, message = "Categoria de ficha inválida.")
    String categoria,
    @Pattern(regexp = Dominio.RE_PRACA_OU_VAZIA, message = "Praça inválida.")
    String praca,
    @Pattern(regexp = Dominio.RE_CONSERVACAO, message = "Conservação inválida.")
    String conservacao,
    @PositiveOrZero(message = "A validade não pode ser negativa.")
    Integer validadeDias,
    @Size(max = 40, message = "Setor inválido.")
    String setor,
    @Min(value = 0, message = "A antecedência da produção deve ser de 0 a 30 dias.")
    @Max(value = 30, message = "A antecedência da produção deve ser de 0 a 30 dias.")
    Integer dnProducao,
    @PositiveOrZero(message = "O tempo de preparo não pode ser negativo.")
    Integer tempoMin,
    @Size(max = 200, message = "Os equipamentos devem ter no máximo 200 caracteres.")
    String equipamentos,
    @Size(max = 300, message = "O armazenamento deve ter no máximo 300 caracteres.")
    String armazenamento,
    Boolean compradoPronto,
    @NotNull(message = "Informe quanto a receita rende.")
    @Positive(message = "Informe quanto a receita rende.")
    Double rendimento,
    @Pattern(regexp = Dominio.RE_UNIDADE_RENDIMENTO, message = "A unidade do rendimento deve ser porções, un, kg ou l.")
    String unidadeRendimento,
    @PositiveOrZero(message = "A porção por convidado não pode ser negativa.")
    Double porcaoPadrao,
    List<@Valid @NotNull Item> itens,
    List<@Valid @NotNull Louca> loucas,
    List<@Valid @NotNull Etapa> etapas,
    List<@Valid @NotNull Componente> componentes,
    @Size(max = 300, message = "A observação de envio deve ter no máximo 300 caracteres.")
    String obsEnvio,
    @Size(max = 4000, message = "O modo de preparo deve ter no máximo 4000 caracteres.")
    String modoPreparo
) {
    public record Item(
        @Positive(message = "Item da receita inválido.") Long insumoId,
        @Positive(message = "Sub-receita inválida.") Long subId,
        @NotNull(message = "Informe a quantidade de cada item da receita.")
        @Positive(message = "Informe a quantidade de cada item da receita.")
        Double qtd,
        @Positive(message = "O fator de correção deve ser maior que zero.") Double fc
    ) {
        @AssertTrue(message = "Cada item é um insumo ou uma sub-receita.")
        public boolean isInsumoOuSubReceita() {
            return (insumoId != null) != (subId != null);
        }
    }

    public record Louca(
        @NotNull(message = "Escolha a louça.") @Positive(message = "Louça inválida.") Long insumoId,
        @NotNull(message = "Informe a quantidade de cada louça.")
        @Positive(message = "Informe a quantidade de cada louça.")
        Double qtd
    ) {}

    public record Etapa(
        @NotBlank(message = "Descreva cada etapa.") @Size(max = 300, message = "A etapa deve ter no máximo 300 caracteres.") String desc,
        @Pattern(regexp = Dominio.RE_PRACA_OU_VAZIA, message = "Praça da etapa inválida.") String praca,
        @PositiveOrZero(message = "Os dias antes do evento não podem ser negativos.") Integer dn
    ) {}

    public record Componente(
        @NotBlank(message = "Informe o nome de cada componente de envio.")
        @Size(max = 200, message = "O componente deve ter no máximo 200 caracteres.")
        String n,
        @Pattern(regexp = Dominio.RE_UNIDADE_COMPONENTE, message = "Unidade do componente inválida.") String u,
        @NotNull(message = "Informe a quantidade de cada componente de envio.")
        @Positive(message = "Informe a quantidade de cada componente de envio.")
        Double q,
        @Pattern(regexp = Dominio.RE_CONSERVACAO, message = "Conservação do componente inválida.") String cons,
        @Size(max = 300, message = "A observação do componente deve ter no máximo 300 caracteres.") String obs
    ) {}
}
