package com.jbkloh.ficha_buffet.dtos.resp;

/** Categoria financeira: uma linha da DRE dentro de um grupo. */
public record CategoriaResponseDTO(Long id, String nome, String grupo, int ordem) {}
