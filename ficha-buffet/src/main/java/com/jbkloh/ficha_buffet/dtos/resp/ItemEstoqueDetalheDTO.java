package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Ficha do item no estoque: saldo, últimos movimentos e fichas que o usam. */
public record ItemEstoqueDetalheDTO(@JsonUnwrapped SaldoItemDTO saldo, List<MovimentoListaItemDTO> movimentos, List<String> usadoEm) {}
