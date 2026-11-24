package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;

/** Libera as ordens do evento para a equipe (true) ou recolhe de volta (false). */
public record LiberarMapaRequestDTO(@NotNull(message = "Informe se libera ou recolhe.") Boolean liberar) {}
