package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

public record PratoDTO(
    String nome,
    String categoria,
    String foto,
    Integer rendQtd,
    String rendUnid,
    Double porPessoa,
    String receita,
    List<IngredienteDTO> ingredientes
) {}