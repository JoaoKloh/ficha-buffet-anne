package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Grupo de opções de um select: cada item é o par [id, rótulo]. */
public record GrupoOpcoesDTO(String grupo, List<List<Object>> itens) {}
