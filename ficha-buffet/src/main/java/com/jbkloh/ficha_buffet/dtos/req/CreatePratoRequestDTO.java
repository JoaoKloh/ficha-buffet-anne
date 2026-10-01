package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreatePratoRequestDTO(

    @NotBlank(message = "O nome do prato é obrigatório.")
    @Size(max = 255, message = "O nome do prato deve ter no máximo 255 caracteres.")
    String nome,

    @NotBlank(message = "A categoria é obrigatória.")
    @Size(max = 100, message = "A categoria deve ter no máximo 100 caracteres.")
    String categoria,

    @URL(message = "A foto deve ser uma URL válida.")
    String foto,

    @NotNull(message = "O rendimento da receita (qtd) é obrigatório.")
    @Positive(message = "O rendimento da receita deve ser maior que zero.")
    Integer rendQtd,

    @NotBlank(message = "A unidade do rendimento é obrigatória.")
    @Size(max = 50, message = "A unidade do rendimento deve ter no máximo 50 caracteres.")
    String rendUnid,

    @Min(value = 0, message = "A sugestão por pessoa não pode ser negativa.")
    Double porPessoa,

    @Size(max = 2000, message = "A receita/modo de preparo deve ter no máximo 2000 caracteres.")
    String receita,

    @NotEmpty(message = "O prato deve conter pelo menos um ingrediente.")
    List<@Valid CreatePratoItemIngredienteDTO> ingredientes

) {}