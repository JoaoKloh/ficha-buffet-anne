package com.jbkloh.ficha_buffet.dtos.resp;

import com.jbkloh.ficha_buffet.model.PratoEntity;

public record ItemPratoResponseDTO(
    Long id,
    String nome,
    String categoria,
    Integer rendQtd,
    String rendUnid,
    Double porPessoa,
    String receita,
    String foto
) {
    public ItemPratoResponseDTO(PratoEntity entity) {
        this(
            entity.getId(),
            entity.getNome(),
            entity.getCategoria(),
            entity.getRendimentoReceita(),
            entity.getUnidade(),
            entity.getRendimentoPessoa(),
            entity.getDescricao(),
            entity.getUrlImagem()
        );
    }
}