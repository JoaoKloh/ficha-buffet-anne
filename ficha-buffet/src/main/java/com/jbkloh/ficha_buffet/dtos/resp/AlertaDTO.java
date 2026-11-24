package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

/** Alerta do Painel. Cada parte é um texto ou { b: "negrito" }; destino diz para onde o alerta leva. */
public record AlertaDTO(String tom, long ord, List<Object> partes, Map<String, Object> destino) {}
