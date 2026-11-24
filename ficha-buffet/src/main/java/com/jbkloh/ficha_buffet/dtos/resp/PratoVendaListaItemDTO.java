package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record PratoVendaListaItemDTO(@JsonUnwrapped PratoVendaResponseDTO prato, CmvDTO cmv, List<String> componentesNomes) {}
