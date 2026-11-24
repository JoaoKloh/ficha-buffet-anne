package com.jbkloh.ficha_buffet.dtos.resp;

/** Pessoa do cadastro da equipe, com a função e a diária padrão usadas na escala em lote. */
public record EquipeResponseDTO(Long id, String nome, String funcaoPadrao, double diariaPadrao, String contato, String pix) {}
