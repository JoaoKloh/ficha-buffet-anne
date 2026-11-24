package com.jbkloh.ficha_buffet.dtos.resp;

/** Extra cobrado no evento, com o lançamento espelho no caixa. */
public record ExtraResponseDTO(Long id, Long eventoId, String descricao, double quantidade, double valorUnitario, Long lancamentoId) {}
