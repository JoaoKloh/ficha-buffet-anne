package com.jbkloh.ficha_buffet.model.json;

/** Uma ação registrada numa ordem de produção: quando (ISO), quem e o quê. */
public record HistoricoOrdem(String em, String quem, String acao) {}
