package com.jbkloh.ficha_buffet.dtos.resp;

import java.time.LocalDate;
import java.util.List;

import com.jbkloh.ficha_buffet.model.EventoEntity;

public record ProducaoResponseDTO(
    Long id,
    String nome,
    Integer quantidade,
    LocalDate data,
    List<ItemPratoResponseDTO> pratos
) {
    public ProducaoResponseDTO(EventoEntity entity) {
        this(
            entity.getId(),
            entity.getNome(),
            entity.getConvidadosPrevistos(),
            entity.getDataEvento(),
            entity.getCardapio() != null 
                ? entity.getCardapio().stream().map(item -> new ItemPratoResponseDTO(item.getPreparacao())).toList() 
                : List.of()
        );
    }
}