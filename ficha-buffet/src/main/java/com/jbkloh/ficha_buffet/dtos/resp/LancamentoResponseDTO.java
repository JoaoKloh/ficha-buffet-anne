package com.jbkloh.ficha_buffet.dtos.resp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEntity;

public record LancamentoResponseDTO(
    Long id,
    String tipo,
    String categoria,
    String descricao,
    BigDecimal valor,
    LocalDate data,
    LocalDate dataFinal,
    List<Long> producaoIds
) {
    public LancamentoResponseDTO(LancamentoEntity entity) {
        this(
            entity.getId(),
            entity.getTipo(),
            entity.getCategoria(),
            entity.getDescricao(),
            entity.getValor(),
            entity.getDataInical(),
            entity.getDataFinal(),
            entity.getProducoes() != null
                ? entity.getProducoes().stream().map(ProducaoEntity::getId).toList()
                : List.of()
        );
    }
}
