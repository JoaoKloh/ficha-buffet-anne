package com.jbkloh.ficha_buffet.dtos.resp;

/** Tipo de custo com realizado mais de 10% acima do previsto; desvio null = havia custo realizado sem previsto. */
public record DesvioCustoDTO(String tipo, double previsto, double realizado, Double desvio) {}
