package com.jbkloh.ficha_buffet.dtos.resp;

import com.jbkloh.ficha_buffet.model.PreparacaoEntity;

public record ItemPratoResponseDTO(
    Long id,
    String nome,
    String categoria,
    Double rendQtd,
    String rendUnid,
    Double porPessoa,
    String receita,
    String foto
) {
    public ItemPratoResponseDTO(PreparacaoEntity entity) {
        this(
            entity.getId(),
            entity.getNome(),
            entity.getCategoria(),
            entity.getRendimento(),
            entity.getUnidadeRendimento(),
            entity.getPorcaoPadrao(),
            entity.getModoPreparo(),
            entity.getUrlImagem()
        );
    }
}