package com.jbkloh.ficha_buffet.dtos.resp;

import java.time.LocalDate;
import java.util.List;

import com.jbkloh.ficha_buffet.model.ProducaoEntity;

public record ProducaoResponseDTO(
    Long id,
    String nome,
    Integer quantidade,
    LocalDate data,
    List<ItemPratoResponseDTO> pratos
) {
    public ProducaoResponseDTO(ProducaoEntity entity) {
        this(
            entity.getId(),
            entity.getNome(),
            entity.getQuantidade(),
            entity.getData(),
            entity.getPratos() != null 
                ? entity.getPratos().stream().map(ItemPratoResponseDTO::new).toList() 
                : List.of()
        );
    }
}