package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Conferência do cardápio colado contra as fichas técnicas. */
public record ImportarCardapioAnaliseResponseDTO(String cabecalho, List<Linha> linhas) {

    public record Linha(String texto, String situacao, List<Sugestao> sugestoes) {}

    public record Sugestao(Long preparacaoId, String nome, double score, String fichaStatus) {}
}
