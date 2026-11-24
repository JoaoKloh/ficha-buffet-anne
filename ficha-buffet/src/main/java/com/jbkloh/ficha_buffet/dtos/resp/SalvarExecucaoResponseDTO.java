package com.jbkloh.ficha_buffet.dtos.resp;

/** envioApagado: a quantidade enviada foi apagada porque a linha deixou de cumprir as condições de saída. */
public record SalvarExecucaoResponseDTO(ExecucaoResponseDTO execucao, boolean envioApagado, Boolean baixaAlterada) {}
