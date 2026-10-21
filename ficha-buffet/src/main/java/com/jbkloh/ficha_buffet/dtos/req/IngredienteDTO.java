package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.model.IngredientesEntity;

public record IngredienteDTO(
    String nome,
    Double qtd,
    String unidade
) {

    public static IngredientesEntity toEntity(IngredienteDTO dto) {
        IngredientesEntity ingrediente = new IngredientesEntity();
        ingrediente.setNome(dto.nome());
        ingrediente.setUnidade(dto.unidade());
        return ingrediente;
    }
}