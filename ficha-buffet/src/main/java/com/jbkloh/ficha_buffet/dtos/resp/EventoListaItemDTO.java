package com.jbkloh.ficha_buffet.dtos.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record EventoListaItemDTO(@JsonUnwrapped EventoResponseDTO evento, String clienteNome) {}
