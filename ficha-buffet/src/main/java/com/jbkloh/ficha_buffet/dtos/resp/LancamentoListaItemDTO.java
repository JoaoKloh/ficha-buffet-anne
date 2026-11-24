package com.jbkloh.ficha_buffet.dtos.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Lançamento com nomes resolvidos e os valores que as listas mostram:
 * unidade efetiva ("" = sem unidade), competência (data do evento para lançamentos
 * de evento), vencimento efetivo, quanto já foi pago, saldo em aberto e situação.
 */
public record LancamentoListaItemDTO(@JsonUnwrapped LancamentoCaixaResponseDTO lancamento, String grupo, String categoriaNome, String eventoNome,
        String unidadeEfetiva, String competencia, String vencimentoEfetivo, double pago, double saldo, String situacaoTom, String situacao, String parte) {}
