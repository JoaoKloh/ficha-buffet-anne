package com.jbkloh.ficha_buffet.dtos.resp;

/** Cliente do buffet. */
public record ClienteResponseDTO(Long id, String nome, String tipo, String documento, String telefone, String email, String origem, String criadoEm) {}
