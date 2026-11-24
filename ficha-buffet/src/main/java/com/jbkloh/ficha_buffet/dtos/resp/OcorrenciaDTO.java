package com.jbkloh.ficha_buffet.dtos.resp;

/** Ocorrência do evento na aba Ocorrências da cozinha. quando = instante do registro (ISO). */
public record OcorrenciaDTO(Long id, Long eventoId, String tipo, String quem, String texto, String acao, String quando, boolean resolvida) {}
