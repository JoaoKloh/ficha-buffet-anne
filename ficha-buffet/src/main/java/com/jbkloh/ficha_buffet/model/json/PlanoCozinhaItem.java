package com.jbkloh.ficha_buffet.model.json;

/** Tarefa do plano da cozinha montado pela gestão. */
public record PlanoCozinhaItem(String k, String desc, String praca, Integer dn, String produtos, String resp) {}
