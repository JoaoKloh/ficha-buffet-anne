package com.jbkloh.ficha_buffet.dtos.resp;

/** Custo dos ingredientes do prato e CMV sobre o preço (nulo sem preço). */
public record CmvDTO(double custo, int faltando, Double cmv) {}
