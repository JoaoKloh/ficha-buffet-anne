package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Lote com nomes resolvidos, situação ([tom, rótulo]) e valor do saldo. */
public record LoteListaItemDTO(@JsonUnwrapped LoteResponseDTO lote, String preparacaoNome, String eventoNome, List<String> situacao, double valor,
        String conservacao) {}
