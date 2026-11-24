package com.jbkloh.ficha_buffet.dtos.resp;

/** Parcela do contrato, com o lançamento espelho no caixa. recebido é o que já entrou pelos pagamentos do lançamento. */
public record ParcelaResponseDTO(Long id, Long eventoId, int numero, String vencimento, double valor, String pagoEm, String forma, Long lancamentoId,
        double recebido) {}
