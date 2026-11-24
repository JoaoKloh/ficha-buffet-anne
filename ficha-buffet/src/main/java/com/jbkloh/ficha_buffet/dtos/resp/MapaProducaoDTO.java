package com.jbkloh.ficha_buffet.dtos.resp;

/** Mapa de produção do evento (ficha mestre). liberado vazio = só a gestão vê as ordens. */
public record MapaProducaoDTO(String gerado, String liberado, String responsavel, String horarios) {}
