package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Linhas conferidas da importação do cardápio por texto. */
public record ImportarCardapioRequestDTO(
    @NotNull(message = "Nenhuma linha para importar.")
    List<@Valid @NotNull Linha> linhas
) {
    public record Linha(
        @NotNull(message = "Linha do cardápio inválida.")
        @Size(max = 200, message = "Cada linha do cardápio deve ter no máximo 200 caracteres.")
        String texto,
        @Positive(message = "Ficha técnica inválida.")
        Long preparacaoId,
        // Quando a linha não casou com nenhuma ficha, cria uma ficha incompleta nesta categoria.
        @Pattern(regexp = Dominio.RE_CATEGORIA_PREPARACAO, message = "Categoria de ficha inválida.")
        String criarCategoria
    ) {}
}
