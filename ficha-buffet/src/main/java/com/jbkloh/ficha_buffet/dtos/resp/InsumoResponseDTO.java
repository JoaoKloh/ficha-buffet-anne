package com.jbkloh.ficha_buffet.dtos.resp;

/** Item de estoque (ingrediente, embalagem, descartável, louça, utensílio ou limpeza). */
public record InsumoResponseDTO(Long id, String nome, String tipo, String categoria, String unidade, String local, String unidadeCompra,
        Double fatorCompra, double estoqueMinimo, double custo, String fornecedor, String atualizadoEm) {}
