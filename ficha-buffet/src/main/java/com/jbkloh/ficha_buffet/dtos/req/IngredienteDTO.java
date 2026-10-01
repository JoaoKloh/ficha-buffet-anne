package com.jbkloh.ficha_buffet.dtos.req;


public record IngredienteDTO(
    String nome,
    Double qtd,
    String unidade
) {}