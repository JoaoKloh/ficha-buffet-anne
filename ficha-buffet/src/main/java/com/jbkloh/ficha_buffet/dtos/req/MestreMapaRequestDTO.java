package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.Size;

/** Ficha mestre do mapa de produção: responsável geral e horários de serviço. */
public record MestreMapaRequestDTO(
    @Size(max = 120, message = "O responsável deve ter no máximo 120 caracteres.") String responsavel,
    @Size(max = 300, message = "Os horários devem ter no máximo 300 caracteres.") String horarios
) {}
