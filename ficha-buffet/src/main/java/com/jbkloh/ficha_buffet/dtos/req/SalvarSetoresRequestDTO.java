package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Setores da cozinha: renomear, ativar/desativar e criar (id vazio). Setor fora da lista fica como está. */
public record SalvarSetoresRequestDTO(@NotEmpty(message = "Informe os setores.") List<@Valid @NotNull Setor> setores) {

    public record Setor(
        @Size(max = 40, message = "Setor inválido.") String id,
        @NotBlank(message = "Informe o nome do setor.") @Size(max = 60, message = "O nome do setor deve ter no máximo 60 caracteres.") String nome,
        Boolean ativo
    ) {}
}
