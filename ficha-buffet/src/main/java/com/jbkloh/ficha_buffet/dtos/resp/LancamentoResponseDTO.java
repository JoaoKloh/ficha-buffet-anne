package com.jbkloh.ficha_buffet.dtos.resp;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.jbkloh.ficha_buffet.model.LancamentoEntity;

public record LancamentoResponseDTO(
    Long id,
    String tipo,
    String categoria,
    String descricao,
    BigDecimal valor,
    LocalDate data
) {
    public LancamentoResponseDTO(LancamentoEntity entity) {
        this(
            entity.getId(),
            entity.getTipo(),
            entity.getCategoria(),
            entity.getDescricao(),
            entity.getValor(),
            entity.getData()
        );
    }
}
