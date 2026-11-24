package com.jbkloh.ficha_buffet.dtos.resp;

/** Custo da receita, custo por unidade de rendimento e quantos itens estão sem preço. */
public record CustoPreparacaoDTO(double total, double porUnid, int faltando) {}
