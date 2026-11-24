package com.jbkloh.ficha_buffet.dtos.resp;

/** Item do plano de produção. k = chave do item no plano; qtd na unidade de rendimento da ficha. */
public record ItemPlanoDTO(String k, Long preparacaoId, double qtd, boolean feito, String feitoEm, Long loteId) {}
