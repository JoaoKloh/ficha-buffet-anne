package com.jbkloh.ficha_buffet.dtos.resp;

/** Linha da lista de planos: quantos itens o plano tem e quantos já foram feitos. */
public record PlanoListaItemDTO(Long id, String titulo, String unidade, String status, String dataInicio, String dataFim, int itens, int feitos) {}
