package com.jbkloh.ficha_buffet.dtos.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Tarefa com os ajustes da cozinha (feito, responsável, dia movido) aplicados. */
public record TarefaCozinhaDTO(@JsonUnwrapped CozinhaProjecaoDTO.Tarefa tarefa, Long eventoId, String codigo, String eventoNome, String eventoData,
        Integer dnEfetivo, String data, boolean feito, boolean movida) {}
