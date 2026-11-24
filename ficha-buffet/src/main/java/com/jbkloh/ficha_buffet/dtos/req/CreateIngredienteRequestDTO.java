package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.jbkloh.ficha_buffet.model.IngredientesEntity;

public record CreateIngredienteRequestDTO(
    
    @NotBlank(message = "O nome do ingrediente é obrigatório.")
    @Size(max = 255, message = "O nome do ingrediente deve ter no máximo 255 caracteres.")
    String nome,

    @NotBlank(message = "A unidade de medida do ingrediente é obrigatória.")
    @Pattern(regexp = "kg|l|un|dz|maço|garrafa|botijão", message = "A unidade deve ser kg, l, un, dz, maço, garrafa ou botijão.")
    String unidade,

    @NotBlank(message = "A categoria do ingrediente é obrigatória.")
    @Pattern(regexp = "Proteínas e frutos do mar|Queijos e laticínios|Hortifruti|Mercearia|Bebidas|Descartáveis|Produtos de limpeza|Outros", message = "Categoria de ingrediente inválida.")
    String categoria,

    @Size(max = 1024, message = "A unidade de medida deve ter no máximo 1024 caracteres.")
    String descricao,

    @PositiveOrZero(message = "O custo não pode ser negativo.")
    Double custo,

    @Size(max = 50, message = "O fornecedo deve ter no máximo 50 caracteres.")
    String fornecedor
) {

    public static IngredientesEntity toEntity(CreateIngredienteRequestDTO dto) {
        IngredientesEntity ingrediente = new IngredientesEntity();
        ingrediente.setCategoria(dto.categoria());
        ingrediente.setNome(dto.nome());
        ingrediente.setUnidade(dto.unidade());
        ingrediente.setCusto(custoDe(dto.custo()));
        ingrediente.setFornecedor(dto.fornecedor());
        return ingrediente;
    }

    /** Custo por unidade com 4 casas; sem custo informado vale zero. */
    public static BigDecimal custoDe(Double custo) {
        return custo == null ? BigDecimal.ZERO : BigDecimal.valueOf(custo).setScale(4, RoundingMode.HALF_UP);
    }
}