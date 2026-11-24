package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

public record EventosResponseDTO(int ativos, int total, List<EventoListaItemDTO> eventos) {}
