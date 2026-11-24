package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Escala completa do evento marcada no checklist: quem está na lista e não estava entra com a
 * função e a diária padrão do cadastro; quem estava e saiu da lista sai da escala. Lista vazia
 * tira todos.
 */
public record AssociarEquipeRequestDTO(
    @NotNull(message = "Informe as pessoas da escala.")
    @Size(max = 500, message = "A escala aceita até 500 pessoas por vez.")
    List<@NotNull(message = "Pessoa da equipe inválida.") @Positive(message = "Pessoa da equipe inválida.") Long> equipeIds
) {}
