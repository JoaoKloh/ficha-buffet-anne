package com.jbkloh.ficha_buffet.model.json;

/** Componente que viaja separado no checklist de envio: nome, unidade, quantidade por unidade do prato, conservação e observação. */
public record ComponenteEnvio(String n, String u, Double q, String cons, String obs) {}
