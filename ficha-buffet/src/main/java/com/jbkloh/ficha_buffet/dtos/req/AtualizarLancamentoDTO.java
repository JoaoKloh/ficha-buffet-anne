package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEntity;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarLancamentoDTO(

    @NotNull(message = "O id do lançamento não pode ser nulo.")
    Long id,

    @NotBlank(message = "O tipo do lançamento é obrigatório.")
    @Pattern(regexp = "receita|despesa", message = "O tipo do lançamento deve ser 'receita' ou 'despesa'.")
    String tipo,

    @NotBlank(message = "A categoria do lançamento é obrigatória.")
    @Size(max = 100, message = "A categoria deve ter no máximo 100 caracteres.")
    String categoria,

    @NotBlank(message = "A descrição do lançamento é obrigatória.")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
    String descricao,

    @NotNull(message = "O valor do lançamento é obrigatório.")
    @Positive(message = "O valor do lançamento deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valor,

    @NotNull(message = "A data do lançamento é obrigatória.")
    LocalDate data,

    @NotNull(message = "A data final do lançamento é obrigatória.")
    LocalDate dataFinal,

    // Opcional: ids das produções vinculadas ao lançamento (nulo ou vazio = nenhuma).
    List<@NotNull(message = "O id da produção não pode ser nulo.") Long> producaoIds
) {

    public static void atualizarEntidade(AtualizarLancamentoDTO req, LancamentoEntity lancamento, List<ProducaoEntity> producoes) {
        lancamento.setTipo(req.tipo());
        lancamento.setCategoria(req.categoria());
        lancamento.setDescricao(req.descricao().trim());
        lancamento.setValor(req.valor());
        lancamento.setDataInical(req.data());
        lancamento.setDataFinal(req.dataFinal());
        lancamento.setProducoes(producoes);
    }
}
