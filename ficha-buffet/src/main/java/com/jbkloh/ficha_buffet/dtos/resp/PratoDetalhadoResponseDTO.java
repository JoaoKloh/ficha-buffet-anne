package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.jbkloh.ficha_buffet.model.PreparacaoEntity;

public record PratoDetalhadoResponseDTO(
    Long id,
    String nome,
    String categoria,
    Double rendQtd,
    String rendUnid,
    Double porPessoa,
    String receita,
    String foto,
    List<IngredienteResponseDTO> ingredientes
) {
    public PratoDetalhadoResponseDTO(PreparacaoEntity entity) {
        this(
            entity.getId(),
            entity.getNome(),
            entity.getCategoria(),
            entity.getRendimento(),
            entity.getUnidadeRendimento(),
            entity.getPorcaoPadrao(),
            entity.getModoPreparo(),
            entity.getUrlImagem(),
            entity.getItensIngredientes() != null 
                ? entity.getItensIngredientes().stream().filter(item -> item.getIngrediente() != null).map(IngredienteResponseDTO::new).toList() 
                : List.of()
        );
    }
}