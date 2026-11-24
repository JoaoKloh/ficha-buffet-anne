package com.jbkloh.ficha_buffet.dtos.resp;

/** Receitas, despesas, resultado e margem (nula sem receita) de um evento. */
public record ResultadoEventoDTO(double rec, double desp, double res, Double mg) {}
