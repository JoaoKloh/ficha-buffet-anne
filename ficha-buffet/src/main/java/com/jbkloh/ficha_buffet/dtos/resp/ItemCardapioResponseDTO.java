package com.jbkloh.ficha_buffet.dtos.resp;

/** Prato do cardápio de um evento. */
public record ItemCardapioResponseDTO(Long id, Long eventoId, Long preparacaoId, double porcao, String status, String praca, String responsavel) {}
