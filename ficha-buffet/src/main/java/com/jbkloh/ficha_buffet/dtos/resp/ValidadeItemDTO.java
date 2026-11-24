package com.jbkloh.ficha_buffet.dtos.resp;

/**
 * Insumo (pela validade da entrada que ainda está no estoque) ou lote de pré-preparo
 * vencido ou vencendo. dias até vencer: negativo = vencido.
 */
public record ValidadeItemDTO(String tipo, String validade, int dias, String nome, String detalhe, double qtd, String unidade, double valor,
        Long insumoId, Long loteId) {}
