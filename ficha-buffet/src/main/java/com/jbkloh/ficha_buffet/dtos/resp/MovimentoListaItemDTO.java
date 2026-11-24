package com.jbkloh.ficha_buffet.dtos.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record MovimentoListaItemDTO(@JsonUnwrapped MovimentoResponseDTO movimento, String insumoNome, String eventoNome) {}
