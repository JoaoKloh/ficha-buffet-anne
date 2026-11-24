package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/** Linhas de uma nota (descrição e código do fornecedor) para sugerir o item do cadastro de cada uma. */
public record NfDeParaRequestDTO(
    String fornecedor,
    @NotNull(message = "Nenhuma linha para conferir.")
    List<@NotNull Linha> itens
) {
    public record Linha(String desc, String cod) {}
}
