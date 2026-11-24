package com.jbkloh.ficha_buffet.dtos.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Linha da tela Clientes: cliente + resumo dos eventos. */
public record ClienteListaItemResponseDTO(@JsonUnwrapped ClienteResponseDTO cliente, int totalEventos, String proximoEvento) {}
