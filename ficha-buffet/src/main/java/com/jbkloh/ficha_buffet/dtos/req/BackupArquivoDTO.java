package com.jbkloh.ficha_buffet.dtos.req;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

/**
 * Arquivo de backup (.json): tudo que está cadastrado, por coleção e id do
 * registro. É o que a exportação devolve e o que a análise e a restauração recebem.
 */
public record BackupArquivoDTO(
    String app,
    String exportadoEm,
    @NotNull(message = "Este arquivo não é um backup do Anne Gestão.")
    Map<String, Map<String, Map<String, Object>>> colecoes
) {}
