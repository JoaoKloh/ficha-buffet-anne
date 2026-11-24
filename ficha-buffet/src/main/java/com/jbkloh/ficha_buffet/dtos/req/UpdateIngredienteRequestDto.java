package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import com.jbkloh.ficha_buffet.model.IngredientesEntity;

public record UpdateIngredienteRequestDto(

    @NotNull(message = "O id do ingrediente não pode ser nulo.") 
    Long id,
    
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

    public static void atualizarEntidade(UpdateIngredienteRequestDto req, IngredientesEntity ingrediente) {
        ingrediente.setCategoria(req.categoria());
        ingrediente.setNome(req.nome());
        ingrediente.setCusto(CreateIngredienteRequestDTO.custoDe(req.custo()));
        ingrediente.setAtualizadoEm(LocalDate.now());
        ingrediente.setFornecedor(req.fornecedor());
        ingrediente.setUnidade(req.unidade());
    }
}