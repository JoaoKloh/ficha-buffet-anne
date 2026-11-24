package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Lote de pré-preparo. validade vazia = sem validade. */
public record LoteResponseDTO(Long id, String codigo, Long preparacaoId, double qtd, double saldo, String unidade, String produzidoEm, String validade,
        String destino, Long eventoId, boolean baixaInsumos, String responsavel, String obs, List<Historico> historico, String criadoEm,
        String unidadeNegocio) {

    /** Uso ou descarte. eventoId: só no uso em evento. */
    public record Historico(String tipo, String data, double qtd, String para, Long eventoId, String obs, String em) {}
}
