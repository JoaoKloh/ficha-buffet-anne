package com.jbkloh.ficha_buffet.model.json;

/** Etapa de produção da ficha: o que fazer, em que praça e quantos dias antes do evento (nulo = a posicionar). */
public record EtapaPreparacao(String desc, String praca, Integer dn) {}
