package com.jbkloh.ficha_buffet.dtos.resp;

/**
 * Situação de uma etapa do evento. feito = auto ou manual; auto = detectada pelo sistema (não dá
 * para desmarcar); manual = marcada à mão e não detectada; em = quando foi marcada ou dispensada (ISO);
 * atrasada = etapa crítica ainda pendente a poucos dias do evento; diasParaEvento = data do evento − hoje.
 */
public record EtapaEventoDTO(String etapa, boolean feito, boolean auto, boolean manual, boolean dispensada, String info, String em, boolean atrasada,
        Long diasParaEvento) {}
