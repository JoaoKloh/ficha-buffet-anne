package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Marca (feito = true) ou desmarca o "pedido feito" de um item da sugestão de compras. */
public record MarcarPedidoRequestDTO(
    @NotNull(message = "Id obrigatório.")
    @Positive(message = "Id obrigatório.")
    Long insumoId,
    @NotNull(message = "Informe se o pedido foi feito.")
    Boolean feito
) {}
