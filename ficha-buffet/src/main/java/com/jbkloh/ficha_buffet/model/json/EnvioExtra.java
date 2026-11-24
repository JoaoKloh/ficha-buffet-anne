package com.jbkloh.ficha_buffet.model.json;

/** Item de serviço, prataria ou descartável do checklist: seção, nome, unidade, modo de cálculo (pax, pax106 ou fix) e valor. */
public record EnvioExtra(String k, String sec, String n, String u, String modo, Double v, String obs) {}
