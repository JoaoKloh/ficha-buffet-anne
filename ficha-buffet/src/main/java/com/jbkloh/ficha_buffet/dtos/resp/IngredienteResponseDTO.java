package com.jbkloh.ficha_buffet.dtos.resp;

import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PratoIngredienteEntity;

public record IngredienteResponseDTO(
    Long id,
    String nome,
    Double qtd,
    String unidade,
    String categoria,
    Double custo,
    String fornecedor
) {
    // Construtor para a listagem global de ingredientes (Catálogo)
    public IngredienteResponseDTO(IngredientesEntity entity) {
        this(
            entity.getId(),
            entity.getNome(),
            null, // Sem quantidade específica pois é o catálogo global
            entity.getUnidade(),
            entity.getCategoria(),
            entity.getCusto(),
            entity.getFornecedor()
        );
    }

    // Construtor para os itens da Ficha Técnica de um Prato (com Quantidade)
    public IngredienteResponseDTO(PratoIngredienteEntity item) {
        this(
            item.getIngrediente().getId(),
            item.getIngrediente().getNome(),
            item.getQuantidade(), // Quantidade específica da receita
            item.getIngrediente().getUnidade(),
            item.getIngrediente().getCategoria(),
            item.getIngrediente().getCusto(),
            item.getIngrediente().getFornecedor()
        );
    }
}