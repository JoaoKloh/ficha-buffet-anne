package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;

/** Marca a ocorrência como resolvida (true) ou reabre (false). */
public record ResolverOcorrenciaRequestDTO(@NotNull(message = "Informe se a ocorrência está resolvida.") Boolean resolvida) {}
