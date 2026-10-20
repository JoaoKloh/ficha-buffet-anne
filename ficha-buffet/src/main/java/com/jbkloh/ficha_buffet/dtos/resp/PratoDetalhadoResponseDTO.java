package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.jbkloh.ficha_buffet.model.PratoEntity;

public record PratoDetalhadoResponseDTO(
    Long id,
    String nome,
    String categoria,
    Integer rendQtd,
    String rendUnid,
    Double porPessoa,
    String receita,
    String foto,
    List<IngredienteResponseDTO> ingredientes
) {
    public PratoDetalhadoResponseDTO(PratoEntity entity) {
        this(
            entity.getId(),
            entity.getNome(),
            entity.getCategoria(),
            entity.getRendimentoReceita(),
            entity.getUnidade(),
            entity.getRendimentoPessoa(),
            entity.getDescricao(),
            entity.getUrlImagem(),
            entity.getItensIngredientes() != null 
                ? entity.getItensIngredientes().stream().map(IngredienteResponseDTO::new).toList() 
                : List.of()
        );
    }
}